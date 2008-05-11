import edu.rit.pj.Comm;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.BarrierAction;
import java.util.List;

/**
 *	Game of Life for SMP machine
*/

public class smpGameOfLife {

	static int maxIterations = 1000;
	static CellLifeRules rules = new ConwayCellLifeRules();
	static boolean board[][];
	static boolean tempBoard[][];
	static List CellList;

	/**
	 * Returns the number of live neighbors
	 * @param board		2D array of booleans corresponding to alive rows
	 * @param row		Row index
	 * @param col		Column index
	 * @return			The number of true elements in board adjacent to (row, col)
	 */
	private static int numLiveNeighbors( boolean[][] board, int row, int col ){
		int countLive = 0;
		boolean onLeftEdge = col == 0;
		boolean onRightEdge = col == board[0].length-1;
		
		// not on top edge
		if ( row > 0 ){
			if ( !onLeftEdge && board[row - 1][col - 1] ) countLive++;
			if ( board[row -1][col] ) countLive++;
			if ( !onRightEdge && board[row - 1][col + 1]) countLive++;
		}
		//not on bottom edge
		if ( row < board.length - 1 ){
			if ( !onLeftEdge && board[row + 1][col - 1] ) countLive++;
			if ( board[row + 1][col] ) countLive++;
			if ( !onRightEdge && board[row + 1][col + 1]) countLive++;
		}

		if ( !onLeftEdge && board[row][col - 1] ) countLive++; 
		if ( !onRightEdge && board[row][col + 1]) countLive++;
		
		return countLive;
	}
	
	private static void displayBoard( boolean[][] board ){
		for (int c=0; c<board[0].length+2; c++ ){
			System.out.print("-");
		}
		System.out.println();
		
		for (int r = 0; r<board.length; r++){
			System.out.print ("|");
			for (int c=0; c<board[0].length; c++ ){
				if (board[r][c]){
					System.out.print("0");
				} else {
					System.out.print(" ");
				}
			}
			System.out.println("|");
		}
		
		for (int c=0; c<board[0].length+2; c++ ){
			System.out.print("-");
		}
		System.out.println();
	}
	
	private static int max( int a, int b ){
		if ( a > b ){
			return a;
		} else {
			return b;
		}
	}
	
	public static void main(String[] args) throws Exception {

		CellList = CellFileReader.readFile( args[0] );

		//find max board size
		int rows = 0;
		int cols = 0;
		
		Cell ThisCell;
		for ( Object ThisObj : CellList ){
			ThisCell = (Cell)ThisObj;
			rows = max( rows, ThisCell.x );
			cols = max( cols, ThisCell.y );
		}
		
		board = new boolean[rows + 1][cols + 1];
		tempBoard = new boolean[rows + 1][cols + 1];

		//create ParallelTeam with anonymous inner class ParallelRegion
		new ParallelTeam().execute (new ParallelRegion(){

			public void run() throws Exception{
				
				int cycle = 0;
				
				//setup intial board configuration 
				//set all cells to false in parallel
				execute( 0, board.length - 1, 
					new IntegerForLoop(){
						public void run( int firstRow, int lastRow ){
							for (int r = firstRow; r<= lastRow; r++){
								for ( int c = 0; c < board[0].length; c++ ){
									board[r][c] = false;
									tempBoard[r][c] = false;
								}
							}
						}
					}
				);
				
				//read in alive cells from CellsList in parallel
				execute( 0, CellList.size() - 1, 
					new IntegerForLoop(){
						public void run( int startIndex, int endIndex ){
							int r, c;
							Cell NextCell;
							for (int i = startIndex; i <= endIndex; i++){
								NextCell = (Cell)CellList.get( i );
								r = NextCell.x;
								c = NextCell.y;
								board[r][c] = true;
								tempBoard[r][c] = true;
							}
						}
					},
				
					//one thread
					new BarrierAction(){
						public void run() throws Exception {
							CellList.clear();
							
							//display initial board setup
							System.out.println("Initial Board Configuration:");
							displayBoard(board);
							
						}
					}
				);
				
				
				do {
					execute( 0, board.length - 1, 
						new IntegerForLoop(){
							public void run( int firstRow, int lastRow ){
								for (int r = firstRow; r<= lastRow; r++){
									for (int c = 0; c < board[0].length; c++){
										tempBoard[r][c] = rules.nextState( board[r][c], 
												numLiveNeighbors( board, r, c ));
									}
								}
							}
						}
					); //end execute
					
					//Copy tempBoard to board
					execute (0, board.length -1, 
						new IntegerForLoop(){
							public void run( int firstRow, int lastRow ){
								for (int r = firstRow; r<= lastRow; r++){
									for (int c = 0; c < board[0].length; c++){
										board[r][c] = tempBoard[r][c];
									}
								}
							}
						}
					); //end execute
					
				} while ( cycle++ < maxIterations );
				//Note: copy board compares every cell. Optimize this to compare only changed cells.
			}	

		});		//end ParallelTeam().execute
		
		//display final board configuration
		System.out.println("Final Board Configuration:");
		displayBoard(board);
	}
}
