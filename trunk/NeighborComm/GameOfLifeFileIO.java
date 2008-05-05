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
	      String fileName,
	      int startCellPos, 
         int numCellsToRead) {
	   
//	   List<Cell> liveCells = new ArrayList<Cell>();
//      int x, y;
//      
//      try {
//         RandomAccessFile dataFile = new RandomAccessFile(fileName, "r");
//         dataFile.seek(startCellPos);
//         
//         String strLine = "";
//         String[] aLineTokens;
//         
//         int nCounter = 0;
//         while ((dataFile.getFilePointer() < dataFile.length()) && (nCounter < numCellsToRead)) 
//         {
//            strLine = dataFile.readLine();
//            aLineTokens = strLine.split(" ");
//            
//            if ((aLineTokens.length == 2) && (aLineTokens[0] != "") && (aLineTokens[1] != ""))
//            {
//               try
//               {
//                  x = Integer.parseInt(aLineTokens[0]);
//                  y = Integer.parseInt(aLineTokens[1]);
//                  
//                  liveCells.add(new Cell(x, y));
//               }
//               catch(NumberFormatException e)
//               {
//                  System.out.println("Start Cell Pos: " + startCellPos + 
//                        " / Received Number Format Exception for Input: (" 
//                        + aLineTokens[0] + ", " + aLineTokens[1] + ")");
//               }
//            }
//            
//            nCounter++;
//         }
//         
//         dataFile.close();
//      }
//      catch (FileNotFoundException fnfe) {
//         fnfe.printStackTrace();
//      }
//      catch (Exception e) {
//         e.printStackTrace();
//      }
//      
//      return liveCells;
	   
		return getLiveCells(new File(fileName), startCellPos, numCellsToRead);
	}
	
	/**
	 * Reads in a file of live cells having the format:
	 * 		x1 y1
	 * 		x2 y2
	 * 		...
	 * 		xn yn
	 * 		<EOF>
	 * @param file File with input in the previously specified format.
	 * @return A list of live cells.  Note, that this list may possibly be
	 * 			empty in the case of an IO error
	 */
	public static List<Cell> getLiveCells(
	      File file,
	      int startCellPos, 
         int cellsToRead) {
		List<Cell> liveCells = new ArrayList<Cell>();
		int x, y;
		
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			while (scanner.hasNextInt()) {
				x = scanner.nextInt();
				
				if (scanner.hasNextInt()) {
					y = scanner.nextInt();
					liveCells.add(new Cell(x, y));
				}	
			}
		} 
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
		return liveCells;
	}

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
		try {
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for (Cell c : liveCells) {
				writer.write(String.format("%s\n", c.toSuccinctString()));
			}
			writer.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
}
