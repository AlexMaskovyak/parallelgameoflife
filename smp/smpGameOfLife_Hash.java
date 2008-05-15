import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.BarrierAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;

public class smpGameOfLife_Hash {
	
	//smp shared variables

	private static int maxIterations;
	private static int iteration;
	private static boolean changed;
	private static Set<Cell> LiveCells;
	private static Map<Cell,Integer> NeighborCount;
	private static Set<Map.Entry<Cell,Integer>> NeighborCountSet;
	private static Iterator<Map.Entry<Cell,Integer>> It;
	private static Iterator<Cell> CellIt;

	private static Vector Births;
	private static Vector Deaths;
	private static Vector EmptyNeighbors;
	private static CellLifeRules rules = new ConwayCellLifeRules();
	private static boolean printBoard;
	
	private static void printUsage(){
		System.out.println("Usage:");
		System.out.println("$java smpGameOfLife inputFileName MaxIterations");
		System.out.println("$java smpGameOfLife inputFileName MaxIterations -p");
		System.out.println("  inputFileName is a serialized ArrayList<Cell>");
		System.out.println("  MaxIterations is the maxiumum number of iterations allowed");
		System.out.println("  -p prints the initial and final board to standard output");
	}
	
	private static void TallyNeighbors( int x, int y, int increase ){
		Cell TallyCell;
		for ( int i=x-1; i<=x+1; i++ ){
		for ( int j=y-1; j<=y+1; j++ ){
			TallyCell = new Cell( i, j );
			if ( (i==x && j==y) ){
				if (!NeighborCount.containsKey( TallyCell )){
					NeighborCount.put( TallyCell, 0 );
				}
			} else {
				if ( NeighborCount.containsKey( TallyCell ) ){
					NeighborCount.put( TallyCell, NeighborCount.get( TallyCell ) + increase);
				} else {
					NeighborCount.put( TallyCell, increase );
				}
			}
		} }
	}
	
	private static int max( int a, int b ){
		if ( a > b ){
			return a;
		} else {
			return b;
		}
	}
	
	private static int min( int a, int b ){
		if ( a < b ){
			return a;
		} else {
			return b;
		}
	}
	
	private static void displayBoard( ){
		int rowMax = 0;
		int rowMin = 0;
		int colMax = 0;
		int colMin = 0;
		
		for ( Cell c: LiveCells ){
			rowMax = max( rowMax, c.x );
			rowMin = min( rowMin, c.x );
			colMax = max( colMax, c.y );
			colMin = min( colMin, c.y );
		}
		
		boolean board[][] = new boolean[rowMax - rowMin + 1][colMax - colMin + 1];
		for (int r=0; r <= rowMax-rowMin; r++){
		for (int c=0; c <= colMax-colMin; c++){
			board[r][c] = false;
		} }
		
		for ( Cell c: LiveCells ){
			board[c.x - rowMin][c.y - colMin] = true;
		}
		
		for (int c=0; c <= colMax - colMin; c++ ){
			System.out.print("-");
		}
		System.out.println("--");
		
		for (int r = 0; r <= rowMax-rowMin; r++){
			System.out.print ("|");
			for (int c=0; c <= colMax-colMin; c++ ){
				if (board[r][c]){
					System.out.print("0");
				} else {
					System.out.print(" ");
				}
			}
			System.out.println("|");
		}
		
		for (int c=0; c <= colMax - colMin; c++ ){
			System.out.print("-");
		}
		System.out.println("--");
	}

	public static void main( String args[] ) throws Exception {
		
		if ( !(args.length == 2 || (args.length == 3/* && args[2]== "-p"*/ ))){
			printUsage();
		} else {
		
		maxIterations = Integer.parseInt( args[1] );
		printBoard = args.length == 3;
		
		ArrayList CellList = CellFileReader.readFile( args[0] );
		LiveCells = Collections.synchronizedSet(
					new HashSet<Cell>( CellList.size() ) );		
		LiveCells.addAll( CellList );
		CellList.clear();
		CellIt = LiveCells.iterator();
		
		NeighborCount = new ConcurrentHashMap<Cell,Integer>( CellList.size() );
		NeighborCountSet= Collections.synchronizedSet(NeighborCount.entrySet());
		Births = new Vector();
		Deaths = new Vector();		
		EmptyNeighbors = new Vector();

		new ParallelTeam().execute (new ParallelRegion(){
			
			public void run() throws Exception{
				//create initial neighbor count in parallel
				execute( 0, LiveCells.size() - 1, 
					new IntegerForLoop(){
						public void run( int firstCell, int lastCell ){
							Cell ThisCell;
							for (int i=firstCell; i<=lastCell; i++){
								ThisCell = CellIt.next();
								TallyNeighbors( ThisCell.x, ThisCell.y, 1 );
							}
						}
					},
					//print initial board configuration
					new BarrierAction(){
						public void run() throws Exception {
							if (printBoard){
								System.out.println("Initial Board Configuration...");
								displayBoard();
							}
						}
					}
				);
				iteration = 0;
				
				do {
					barrier(new BarrierAction() {
						public void run() throws Exception{
							changed = false;
							++iteration;
							It = NeighborCountSet.iterator();
						}
					});

					//populate births and death lists
					execute ( 0, NeighborCount.size() - 1, 
						new IntegerForLoop() {
							List<Cell> ThreadBirths = new ArrayList<Cell>();
							List<Cell> ThreadDeaths = new ArrayList<Cell>();
							List<Cell> ThreadEmptyNeighbors = new ArrayList<Cell>();
							boolean threadChanged = false;
							public void run( int firstCell, int lastCell ){
								Map.Entry<Cell,Integer> NextEntry;
								Cell TempCell;
								int neighbors;
								for (int i=firstCell; i<=lastCell; i++){
									NextEntry = It.next();
									TempCell = NextEntry.getKey();
									neighbors = NextEntry.getValue();
									if ( neighbors == 0 ){
										ThreadEmptyNeighbors.add( TempCell );
										if ( LiveCells.contains( TempCell ) 
												&& rules.dies( 0 )){
											ThreadDeaths.add( TempCell );
											threadChanged = true;
										}
									} else if ( LiveCells.contains( TempCell )){
									// alive and at least one neighbor
										if ( rules.dies( neighbors )){
											ThreadDeaths.add( TempCell );
											threadChanged = true;
										}
									} else if ( rules.isBorn( neighbors )){
										ThreadBirths.add( TempCell );
										threadChanged = true;
									}
								}
							}
							// public per thread lists to shared lists
							public void finish(){
								if ( threadChanged ){
									changed = true;
								}
								if (!ThreadBirths.isEmpty()){
									Births.addAll( ThreadBirths );
								}
								if (!ThreadDeaths.isEmpty()){
									Deaths.addAll( ThreadDeaths );
								}
								if (!ThreadEmptyNeighbors.isEmpty()){
									EmptyNeighbors.addAll( ThreadEmptyNeighbors );
								}
							}
						}
					);

					//remove alone neighbors from Neighbor Count
					execute ( 0, EmptyNeighbors.size() - 1,
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell ){
								for (int i=firstCell; i<=lastCell; i++){
									NeighborCount.remove( EmptyNeighbors.get(i) );
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								EmptyNeighbors.clear();
							}
						}
					);

					//create new cells
					execute ( 0, Births.size() - 1,
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell ){
								Cell TempCell;
								for (int i=firstCell; i<=lastCell; i++){
									TempCell = (Cell)Births.get(i);
									LiveCells.add( TempCell );
									System.out.print("a");
									TallyNeighbors( TempCell.x, TempCell.y, 1 );
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
					execute ( 0, Deaths.size() - 1,
						new IntegerForLoop(){
							public void run( int firstCell, int lastCell ){
								Cell TempCell;
								for (int i=firstCell; i<=lastCell; i++){
									TempCell = (Cell)Deaths.get(i);
									LiveCells.remove( TempCell );
									TallyNeighbors( TempCell.x, TempCell.y, -1 );
								}
							}
						},
						new BarrierAction() {
							public void run() throws Exception{
								Deaths.clear();
							}
						}
					);


				} while ( changed && iteration < maxIterations );
			}
		}); //end parallel team

		if (printBoard){
			System.out.println("Final Board Configuration (" + iteration + 
					" iterations)...");
			displayBoard();
		}
	}
	} //end main
}