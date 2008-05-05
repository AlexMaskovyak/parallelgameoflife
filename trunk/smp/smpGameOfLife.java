import edu.rit.pj.Comm;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.IntegerForLoop;
import java.util.Scanner;

/**
 *	Game of Life for SMP machine
*/

public class smpGameOfLife {

	static int maxIterations = 1000;
	static CellLifeRules rules = new ConwayCellLifeRules();
	static boolean board[][];
	static boolean tempBoard[][];

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
	
		/**
	 * Copys one boolean matrix to another of the same size
	 * @param copyFrom	The source matrix
	 * @param copyTo	The destination matrix
	 * @return 			True if any cells changed state
	 */
/*	private static boolean copyBoard( boolean[][] copyFrom, boolean[][] copyTo ){
		boolean changed = true;
		for ( int r=0; r < copyFrom.length; r++ ){
			for ( int c=0; c < copyFrom[0].length; c++ ){
				if (copyTo[r][c] != copyFrom[r][c]) {
					changed = true;
					copyTo[r][c] = copyFrom[r][c];
				}
			}
		}
		return changed;
	}*/
	
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
	
	public static void main(String[] args) throws Exception {

		//Comm.init (args);
		Scanner sc = new Scanner(System.in);

		//read board size from standard input
		int boardRows = sc.nextInt();
		int boardCols = sc.nextInt();
		
		board = new boolean[boardRows][boardCols];
		tempBoard = new boolean[boardRows][boardCols];

		//initalize board to all dead cells
		for (int r=0; r<boardRows; r++ ){
			for (int c=0; c<boardCols; c++ ){
				board[r][c] = false;
				tempBoard[r][c]=false;
			}
		}
		
		//read alive cells from standard input
		int numAliveCells, row, col;
		numAliveCells = sc.nextInt();
		
		for (int i=0; i<numAliveCells; i++ ){
			row = sc.nextInt();
			col = sc.nextInt();
			board[row][col] = true;
			tempBoard[row][col]=true;
		}
		sc.close();

		//display initial board setup
		System.out.println("Initial Board Configuration:");
		displayBoard(board);
		
		//create ParallelTeam with anonymous inner class ParallelRegion
		new ParallelTeam().execute (new ParallelRegion(){

			public void run() throws Exception{
				
				int cycle = 0;
				do {
					execute (0, board.length-1, new IntegerForLoop(){
						public void run( int firstRow, int lastRow ){
							for (int r = firstRow; r<= lastRow; r++){
								for (int c = 0; c < board[0].length; c++){
									tempBoard[r][c] = rules.nextState( board[r][c], 
											numLiveNeighbors( board, r, c ));
								}
							}
						}
					}); //end execute
					
					//Copy tempBoard to board
					execute (0, board.length-1, new IntegerForLoop(){
						public void run( int firstRow, int lastRow ){
							for (int r = firstRow; r<= lastRow; r++){
								for (int c = 0; c < board[0].length; c++){
									board[r][c] = tempBoard[r][c];
								}
							}
						}
					}); //end execute
					
				} while ( cycle++ < maxIterations );
				//Note: copy board compares every cell. Optimize this to compare only changed cells.
			}	

		});		//end ParallelTeam().execute
		
		//display final board configuration
		System.out.println("Final Board Configuration:");
		displayBoard(board);
	}
}
