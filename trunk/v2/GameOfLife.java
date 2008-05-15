import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.rit.pj.Comm;

/**
 * Handles the execution of the game of life.
 * @author Alex Maskovyak
 *
 */
public class GameOfLife {

	/**
	 * Main executable method for the game of life.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// 
		// Setup Parallel Java objects
		//
		Comm.init(args);
		Comm commWorld = Comm.world();
		
		//
		// read inputs from main
		//
		int numGridRows = Integer.parseInt(args[0]);
		int numGridCols = Integer.parseInt(args[1]);
		int iterationsToRun = Integer.parseInt(args[2]);
		String liveCellInputFile = args[3];
		
		//
		// create Game of Life objects
		//
		
		// specify rules to use
		// specify interconnection framework
		// specify simulation environment
		CellLifeRules rules = new ConwayCellLifeRules();
		CellNeighborhood neighborhood = 
			new ConwayCellNeighborhood(0, numGridCols, 0, numGridRows);
		File cellsFile = new File("glider.txt");

		// create timing items
		long startTime, endTime, runTime;

		//System.out.println("Getting live cells.");
		
		List<Cell> livingCells = GameOfLifeFileIO.getLiveCells(cellsFile);
		
		//System.out.printf("Have live cells: %d\n", livingCells.size());
		
		// create objects, distribute data and perform simulation
		// better start our time
		startTime = System.nanoTime();
		
		//System.out.println("Make simulators...");
		
		GameOfLifeSimulator simulator = 
			new NeighborCommGameOfLifeSimulator(
					rules, 
					neighborhood, 
					commWorld,
					cellsFile);
		//GameOfLifeSimulator simulator = new SequentialGameOfLifeSimulator(livingCells, rules, neighborhood);
		
		//if (commWorld.rank() == 1) {
		//	System.exit(0);
		//}
		
		System.out.printf("%d cells alive\n", simulator.getLivingCellCount());
		for (int i = 0; i < iterationsToRun; ++i) {
			simulator.performSimulation();
			//System.out.printf("Iteration %d, %d cells alive\n", i, simulator.getLivingCellCount());
			System.out.printf("Iteration %d, %d: %s cells alive\n", i, commWorld.rank(), simulator.getLivingCellCount());
		}	
		
		//System.out.printf("%d has %d cells alive\n", commWorld.rank(), simulator.getLivingCellCount());
		
		for (Cell c : simulator.getCurrentState()) {
			System.out.printf("%d: %s\n", commWorld.rank(), c);
		}
		
		// display running time
		endTime = System.nanoTime();
		runTime = endTime - startTime;
		System.out.printf("Runtime: %dms\n", 
				convertNanoSecondsToMilliseconds(runTime));
		
		
		
		// for testing purposes
//		List<Cell> liveCells = simulator.getCurrentState();/
//		for (Cell c : liveCells) {
//			System.out.printf("%d: %s\n", commWorld.rank(), c);
//		}

		// run the glider simulation as an example
//		startTime = System.nanoTime();
//		GameOfLife.gliderTestCase(simulator);


		
		/*		List<Cell> liveCells = GameOfLifeFileIO.getLiveCells("glider.txt");
		Cell[] liveCellArray = liveCells.toArray(new Cell[liveCells.size()]);
		//java.util.Arrays.sort(liveCellArray, Cell.ColumnComparator);
		
		for (Cell cell : liveCellArray) {
			System.out.printf("%s\n", cell.toString());
		}
*/

	}

	/**
	 * Demonstration of the simulation on a glider object.  Also shows how
	 * one would go about reading input, simulating, etc.
	 * @param simulator Simulator to perform Game of Life computations.
	 */
	private static void gliderTestCase(GameOfLifeSimulator simulator) {
		// read-in list of live cells
		List<Cell> livingCells = GameOfLifeFileIO.getLiveCells("glider.txt");
		simulator.addLivingCells(livingCells);
		
		// for debugging, show me the glider pattern we read in
		for (Cell c : livingCells) {
			System.out.printf("%s\n", c);
		}
		System.out.println();
		
		// go ahead and run an iteration
		simulator.performSimulation();
		
		// lets see the result
		livingCells = simulator.getCurrentState();
		for (Cell cell : livingCells) {
			System.out.printf("%s\n", cell);
		}
		System.out.println();
		
		// again!
		simulator.performSimulation();
		livingCells = simulator.getCurrentState();
		for (Cell cell : livingCells) {
			System.out.printf("%s\n", cell);
		}
		
		// sweet it works!

	}
	
	/**
	 * Displays the parameter requirements for running this program.
	 * 
	 */
	public static void displayUsage() {
		// TODO: Implement this method.
	}
	
	/**
	 * Converts nanoseconds to milliseconds.
	 * @param nanoSeconds Number of nanoseconds to convert.
	 * @return The provided number converted into milliseconds.
	 */
	public static long convertNanoSecondsToMilliseconds(long nanoSeconds) {
		long nanosecondsInAMillisecond = 1000000;
		return nanoSeconds / nanosecondsInAMillisecond;
	}
}
