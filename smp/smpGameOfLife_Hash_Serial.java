//import edu.rit.pj.Comm;
//import edu.rit.pj.ParallelTeam;
//import edu.rit.pj.ParallelRegion;
//import edu.rit.pj.IntegerForLoop;
//import edu.rit.pj.BarrierAction;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class smpGameOfLife_Hash_Serial{
	
	private static int maxIterations = 1000;
	
	private static Set<Cell> LiveCells;
	private static Map<Cell,Integer> NeighborCount;
	private static List<Cell> Births;
	private static List<Cell> Deaths;
	private static List<Cell> EmptyNeighbors;
	static CellLifeRules rules = new ConwayCellLifeRules();


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

	public static void main( String args[] ){
		
		ArrayList CellList = CellFileReader.readFile( args[0] );
		LiveCells = new HashSet<Cell>( CellList.size() );		
		NeighborCount = new HashMap<Cell,Integer>( CellList.size() );

		Cell ThisCell;
		for (Object c: CellList){
			ThisCell = (Cell)c;
			LiveCells.add( ThisCell );
					
			//update NeighborCounts
			TallyNeighbors( ThisCell.x, ThisCell.y, 1 );
		}

		CellList.clear();
		
		System.out.println("Initial Board Configuration...");
		displayBoard();

		//create initial neighbor count
		int iteration = 0;
		Births = new ArrayList<Cell>();
		Deaths = new ArrayList<Cell>();
		EmptyNeighbors = new ArrayList<Cell>();
		Cell TempCell;
		boolean changed;
		do {
			changed = false;
			//populate births and death lists
			for( Map.Entry<Cell,Integer> cellIntPair: NeighborCount.entrySet() ){
				if ( cellIntPair.getValue() == 0 ){
					EmptyNeighbors.add( cellIntPair.getKey() );
					//Cell is alive and dies
					if ( LiveCells.contains( cellIntPair.getKey() ) && rules.dies( 0 )){
						Deaths.add( cellIntPair.getKey() );
						changed = true;
					}
				} else if ( LiveCells.contains( cellIntPair.getKey() )){
				// alive and at least one neighbor
					if ( rules.dies( cellIntPair.getValue() )){
						Deaths.add( cellIntPair.getKey() );
						changed = true;
					}
				} else if ( rules.isBorn( cellIntPair.getValue() )){
				//not alive and at least one neighbor
					Births.add( cellIntPair.getKey() );
					changed = true;
				}
			}

			//remove empty neighbors counts
			for ( Cell C: EmptyNeighbors ){
				NeighborCount.remove( C );
			}
			EmptyNeighbors.clear();;

			//create new cells;
			while( !Births.isEmpty() ){
				TempCell = Births.remove( Births.size() - 1 );
				LiveCells.add( TempCell );
				
				TallyNeighbors( TempCell.x, TempCell.y, 1 );
			}

			//remove dead cells;
			while( !Deaths.isEmpty() ){
				TempCell = Deaths.remove( Deaths.size() - 1 );
				LiveCells.remove( TempCell );
				
				TallyNeighbors( TempCell.x, TempCell.y, -1 );
			}

		} while ( changed && iteration++ < maxIterations );
		
		System.out.println("Final Board Configuration (" + iteration + " iterations)...");
		displayBoard();
	}
}