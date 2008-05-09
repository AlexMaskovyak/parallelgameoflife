import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.RandomAccessFile;

/**
 * Handles reading Game of Life files.
 * @author Alex Maskovyak
 *
 */
public abstract class GameOfLifeFileIO {

	/**
	 * Reads in a file of live cells having the format:
	 * 		x1 y1
	 * 		x2 y2
	 * 		...
	 * 		xn yn
	 * 		<EOF>
	 * @param fileName Filename with input in the previously specified format.
	 * @return A list of live cells.  Note, that this list may possibly be
	 * 			empty in the case of an IO error
	 */	
	public static List<Cell> getLiveCells(
	      String filename, int numLiveCells) {
	   
	   List<Cell> liveCells = new ArrayList<Cell>();
	   
	   try
      {
         Scanner readBuffer = new Scanner(new File(filename));
         
         String strLine = "";
         String[] aLineTokens;
         
         for (int i = 0; i < numLiveCells; i++)
         {
            strLine = readBuffer.nextLine();
            aLineTokens = strLine.split(" ");
            
            liveCells.add(new Cell(Integer.parseInt(aLineTokens[0]), Integer.parseInt(aLineTokens[1])));
         }
      }
      catch (FileNotFoundException fnfe)
      {
         fnfe.printStackTrace();
      }
      
      return liveCells;  
	}
	
//	/**
//	 * Reads in a file of live cells having the format:
//	 * 		x1 y1
//	 * 		x2 y2
//	 * 		...
//	 * 		xn yn
//	 * 		<EOF>
//	 * @param file File with input in the previously specified format.
//	 * @return A list of live cells.  Note, that this list may possibly be
//	 * 			empty in the case of an IO error
//	 */
//	public static List<Cell> getLiveCells(
//	      File file) {
////		List<Cell> liveCells = new ArrayList<Cell>();
////		int x, y;
////		
////		Scanner scanner = null;
////		try {
////			scanner = new Scanner(file);
////			while (scanner.hasNextInt()) {
////				x = scanner.nextInt();
////				
////				if (scanner.hasNextInt()) {
////					y = scanner.nextInt();
////					liveCells.add(new Cell(x, y));
////				}	
////			}
////		} 
////		catch (FileNotFoundException fnfe) {
////			fnfe.printStackTrace();
////		}
////		catch (Exception e) {
////			e.printStackTrace();
////		}
////		finally {
////			if (scanner != null) {
////				scanner.close();
////			}
////		}
////		
////		return liveCells;
//	   
//	   
//	   
//	}

	/**
	 * Outputs the specified list of live cells to the filename in the format
	 * of :
	 * 		Cell1.x Cell1.y
	 * 		Cell2.x Cell2.y
	 * 		...
	 * 		Celln.x Celln.y
	 * 		<EOF>
	 * @param live Cells List of Cells to output.
	 * @param fileName Destination filename for output written in the specified
	 * 					format.
	 */
	public static void writeLiveCells(List<Cell> liveCells, String fileName) {
		writeLiveCells(liveCells, new File(fileName));
	}
	
	/**
	 * Outputs the specified list of live cells to the specified file in the
	 * format of :
	 * 		Cell1.x Cell1.y
	 * 		Cell2.x Cell2.y
	 * 		...
	 * 		Celln.x Celln.y
	 * 		<EOF>
	 * @param live Cells List of Cells to output.
	 * @param file Destination file for output written in the specified format.
	 */
	public static void writeLiveCells(List<Cell> liveCells, File file) {
//		try {
//			file.createNewFile();
//			FileWriter writer = new FileWriter(file);
//			for (Cell c : liveCells) {
//				writer.write(String.format("%s\n", c.toSuccinctString()));
//			}
//			writer.close();
//		} 
//		catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
	}
}
