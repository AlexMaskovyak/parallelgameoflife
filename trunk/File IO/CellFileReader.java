/**
 * Reads a Game of Life input file
 * @author Steve Baylor
 *
 */

import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CellFileReader {
	
	/**
	 * Reads a Game of Life Input file
	 * @param inputFilename	Filename to read
	 * @return 				Array<List>
	 */
	
	public static ArrayList readFile( String inputFilename ){
		
		ArrayList CellList = null;
		
		try {
			ObjectInputStream inObject = new ObjectInputStream( 
					new FileInputStream( inputFilename ) );
			CellList = (ArrayList)inObject.readObject();
			inObject.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		
		return CellList;
	}
}
