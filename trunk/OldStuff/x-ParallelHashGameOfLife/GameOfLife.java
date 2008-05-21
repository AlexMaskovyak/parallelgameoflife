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
      int numLivingCellsInFile = Integer.parseInt(args[2]);
      int numIterationsToRun = Integer.parseInt(args[3]);
      String strInputFilename = args[4];
		
		//
		// create Game of Life objects
		//
		
		// specify rules to use
		// specify interconnection framework
		// specify simulation environment
		CellLifeRules rules = new ConwayCellLifeRules();
		CellNeighborhood neighborhood = 
			new ConwayCellNeighborhood(0, numGridCols, 0, numGridRows);
		File cellsFile = new File(strInputFilename);

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
		
		
		System.out.printf("%d cells alive\n", simulator.getLivingCellCount());
		for (int i = 0; i < numIterationsToRun; ++i) {
			simulator.performSimulation();
			//System.out.printf("Iteration %d, %d: %s cells alive\n", i, commWorld.rank(), simulator.getLivingCellCount());
		}	
		
		System.out.printf("Proc %d: %s cells alive\n", commWorld.rank(), simulator.getLivingCellCount());
      
		
		//System.out.printf("%d has %d cells alive\n", commWorld.rank(), simulator.getLivingCellCount());
		
//		for (Cell c : simulator.getCurrentState()) {
//			System.out.printf("%d: %s\n", commWorld.rank(), c);
//		}
		
		// display running time
		endTime = System.nanoTime();
		runTime = endTime - startTime;
		System.out.printf("Runtime: %dms\n", 
				convertNanoSecondsToMilliseconds(runTime));

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
