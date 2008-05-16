import edu.rit.pj.Comm;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.BarrierAction;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;


/**
 *	Game of Life for SMP machine
*/

public class smpGameOfLife {

	static int maxIterations;
	static CellLifeRules rules = new ConwayCellLifeRules();
	static boolean board[][];
	static ArrayList CellList;
	static boolean changed;
	static int cycle;
	static Vector Births;
	static Vector Deaths;
	static boolean printBoard;
	
	private static void printUsage(){
		System.out.println("Usage:");
		System.out.println("$java smpGameOfLife inputFileName MaxIterations");
		System.out.println("$java smpGameOfLife inputFileName MaxIterations -p");
		System.out.println("  inputFileName is a serialized ArrayList<Cell>");
		System.out.println("  MaxIterations is the maxiumum number of iterations allowed");
		System.out.println("  -p prints the initial and final board to standard output");
	}
	
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

		if ( !(args.length == 2 || (args.length == 3/* && args[2]== "-p"*/ ))){
			printUsage();
		}
		else{
		maxIterations = Integer.parseInt( args[1] );
		printBoard = args.length == 3;
		CellList = CellFileReader.readFile( args[0] );

		long startTime, endTime;
		startTime = System.nanoTime();

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
		Births = new Vector();
		Deaths = new Vector();
		cycle = 0;		
		//System.out.println( "Rows " + board.length + ", Col " + board[0].length ); 
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
							Cell NextCell;
							for (int i = startIndex; i <= endIndex; i++){
								NextCell = (Cell)CellList.get( i );
								board[NextCell.x][NextCell.y] = true;
							}
						}
					},
				
					new BarrierAction(){
						public void run() throws Exception {
							CellList.clear();
							
							//display initial board setup
							if (printBoard){
								System.out.println("Initial Board Configuration:");
								displayBoard(board);
							}
						}
					}
				);
				
				do {
					barrier(new BarrierAction() {
						public void run() throws Exception{
							changed = false;
							//Changed.set( false );
							++cycle;
						}
					});

					execute( 0, board.length - 1, 
						new IntegerForLoop(){
							
							boolean threadChanged = false;
							List<Cell> threadBirths = new ArrayList<Cell>();
							List<Cell> threadDeaths = new ArrayList<Cell>();

							public void run( int firstRow, int lastRow ){
								//System.out.println(firstRow + " " + lastRow);
								for (int r = firstRow; r<= lastRow; r++){
									for (int c = 0; c < board[0].length; c++){
										
										if ( board[r][c] ){
											if ( rules.dies(numLiveNeighbors( board, r, c ))){
												threadDeaths.add(new Cell(r,c));
												threadChanged = true;
											}
										} else {
											if ( rules.isBorn(numLiveNeighbors( board, r, c ))){
												threadBirths.add(new Cell(r,c));
												threadChanged = true;
											}
										}
									}
								}
							}
							public void finish(){
								if ( threadChanged ){
									changed = true;
								}
								if (!threadBirths.isEmpty()){
									Births.addAll( threadBirths );
								}
								if (!threadDeaths.isEmpty()){
									Deaths.addAll( threadDeaths );
								}
							}
						}
					); //end execute
					
					
					//Birth new Cells
					execute (0, Births.size() - 1, 
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell){
								List threadBirths = Births.subList(firstCell, lastCell+1);
								Cell TempCell;
								for (Object obj: threadBirths){
									TempCell = (Cell)obj;
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
								List threadDeaths = Deaths.subList(firstCell, lastCell+1);
								Cell TempCell;
								for (Object obj: threadDeaths){
									TempCell = (Cell)obj;
									board[TempCell.x][TempCell.y]=false;
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								Deaths.clear();
							}
						}
					); //end execute

				} while ( cycle < maxIterations && /*Changed.get()*/ changed );
			}	

		});		//end ParallelTeam().execute
		
		//display final board configuration
		if (printBoard){
			System.out.println("Final Board Configuration (" + (cycle-1) + " iterations)...");
			displayBoard( board );
		}
		endTime = System.nanoTime();
		System.out.println("Time: " + (endTime - startTime)/1000000 + " ms" );
	}
}
}
