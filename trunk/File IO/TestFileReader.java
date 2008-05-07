import java.util.List;
import java.util.ArrayList;

public class TestFileReader {
	public static void main (String[] args){
		
		List CellList = new ArrayList<Cell>();
		
		CellList = CellFileReader.readFile( args[0] );
		
		Cell NextCell;
		while ( ! CellList.isEmpty() ){
			NextCell = (Cell)CellList.remove(CellList.size() - 1 );
			System.out.print("<" + NextCell.getRow() + "," +
					NextCell.getCol() + "> " );
		}
	}
}