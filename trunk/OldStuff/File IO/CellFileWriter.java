import java.util.List;
import java.util.ArrayList;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * CellFileWriter serializes a List<Cell> into a file
 */

public class CellFileWriter{

	private List<Cell> CellList;
	
	/**
	 * Creates a List of capacity numCells
	 * @param numCells
	 */
	
	public CellFileWriter( int numCells ){
		CellList = new ArrayList<Cell>(numCells);
	}
	
	/**
	 * Adds a cell to the list
	 * @param row
	 * @param column
	 */
	
	public void addCell( int row, int column ){
		CellList.add( new Cell( row, column ));
	}

	/**
	 * Creates a persistant CellList object in an output file
	 * @param outputFilename
	 */
	
	public void writeToFile( String outputFilename ){

		FileOutputStream outFile;
		ObjectOutputStream outObject;		

		try{
			outFile = new FileOutputStream( outputFilename );
			outObject = new ObjectOutputStream( outFile );
			outObject.writeObject( CellList );
			outObject.close();
		}
		catch( IOException ex ){
			ex.printStackTrace();
		}
	}
}
