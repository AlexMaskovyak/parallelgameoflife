import edu.rit.pj.Comm;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.BarrierAction;
import java.util.List;
import java.util.ArrayList;

/**
 *	Game of Life for SMP machine
*/

public class smpGameOfLife {

	static int maxIterations = 1000;
	static CellLifeRules rules = new ConwayCellLifeRules();
	static boolean board[][];
	static ArrayList CellList;
	static boolean changed;
	static int cycle;
	static List<Cell> Births;
	static List<Cell> Deaths;
	
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
		Births = new ArrayList<Cell>();
		Deaths = new ArrayList<Cell>();
		cycle = 0;
		
		//create ParallelTeam with anonymous inner class ParallelRegion
		new ParallelTeam().execute (new ParallelRegion(){
			
			public void run() throws Exception{
				
				//setup intial board configuration 
				//set all cells to false in parallel
				execute( 0, board.length - 1, 
					new IntegerForLoop(){
						public void run( int firstRow, int lastRow ){
							for (int r = firstRow; r<= lastRow; r++){
								for ( int c = 0; c < board[0].length; c++ ){
									board[r][c] = false;
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
					//Changed.set( false );
					changed = false;
					execute( 0, board.length - 1, 
						new IntegerForLoop(){
							boolean threadChanged = false;
							public void run( int firstRow, int lastRow ){
								for (int r = firstRow; r<= lastRow; r++){
									for (int c = 0; c < board[0].length; c++){
										
										if ( board[r][c] ){
											if ( rules.dies(numLiveNeighbors( board, r, c ))){
												Deaths.add(new Cell(r,c));
												threadChanged = true;
											}
										} else {
											if ( rules.isBorn(numLiveNeighbors( board, r, c ))){
												Births.add(new Cell(r,c));
												threadChanged = true;
											}
										}
									}
								}
							}
							public void finish(){
								if (threadChanged ){
									changed = true;
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								cycle++;
							}
						}
					); //end execute
					
					
					//Birth new Cells
					execute (0, Births.size() -1, 
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell){
								Cell TempCell;
								for (int i=firstCell; i<=lastCell; i++){
									TempCell=Births.get(i);
									board[TempCell.x][TempCell.y]=true;
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								Births.clear();
							}
						}
					);
					
					//kill dead cells
					execute (0, Deaths.size() - 1,
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell){
								Cell TempCell;
								for (int i=firstCell; i<=lastCell; i++){
									TempCell = Deaths.get(i);
									board[TempCell.x][TempCell.y]=false;
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								Deaths.clear();
								cycle++;
								//System.out.println(cycle);
							}
						}
					); //end execute

				} while ( cycle < maxIterations /* && changed */);
			}	

		});		//end ParallelTeam().execute
		
		//display final board configuration
		System.out.println("Final Board Configuration (" + cycle + " iterations)...");
		displayBoard(board);
	}
}
