import java.util.Scanner;


//Translates input from cell locations, into a Serialized ArrayList<Cell>
//The output file must be the first argument passed into main or the first line of input.
//The rest of the input is the number of input cells, followed by cell 1 row, cell 1 col, cell 2 row, cell 2 col, etc...

public class MakeInputFromFile{
	
	public static void main( String[] args ){
		
		String outFilename;
		Scanner sc = new Scanner( System.in );
		
		if ( args.length == 0 ){	//output filename not specified in arguments -> look in standard input
			
			if ( sc.hasNextInt() ){ //output filename not found in standard input
				outFilename = null;
			} else {
				outFilename = sc.nextLine();
			}
		} else {
			outFilename = args[0];
		}
		
		if ( outFilename == null ){
			System.err.println("Must Specify output filename as an argument or in the first line of input");
		} else {
			
			int inputSize = sc.nextInt();
			
			CellFileWriter Cells = new CellFileWriter( inputSize );

			int row, col;		

			for ( int i=0; i < inputSize; i++ ){
				row = sc.nextInt();
				col = sc.nextInt();

				Cells.addCell( row,col );
			}
			sc.close();
			
			Cells.writeToFile( outFilename );
		}
	}
}
