This is currently set up for the "Cell.java" that's in this folder. We would need to update the other "Cell.java" to implement "java.io.Serializable" or make the other "Cell.java" a superclass of this one and rename it.

// Generating GOL input files.

CellFileWriter.java:
-Cell objects are fed into an instance of CellFilewriter with the addCell( int, int) function.
-The bytecode of an ArrayList<Cell> is generated with the function writeToFile(String filename)

MakeInputFromFile.java: An example the usage of CellFileWriter.
-In this example, CellFileWriter is used to generate bytecode of an ArrayList<Cell> by reading in cell row and columns from an input text file.
-"testInput.txt" is an example of the text file read by MakeInputFromFile.
-Other possible ways to use CellFileWriter, are randomly generatation, user interation with a GUI, etc.


// Reading in GOL input files

CellReader.java:
-A static function readFile(String filename) returns an ArrayList (of Cell Objects). Filename is the name of a file that was created with "CellFileWriter.writeToFile".

TestFileReader.java:
-Demonstrates the usage of CellReader. It reads in a file and prints the cells.

Steve

