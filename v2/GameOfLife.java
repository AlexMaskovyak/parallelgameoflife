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

	public enum EngineType { SEQ, MP, DYN_MP, SMP };
	public static final int COMMAND_LINE_LENGTH = 5;
	public static final String USAGE = 
		"java -Dpj.np=[num processors] GameOfLife " +
		"[num rows] [num columns] [iterations] [input filename]" +
		"[engine type: {SEQ, MP, DYN_MP, SMP}]";
	
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
		checkUsage(args);
		
		int numGridRows = Integer.parseInt(args[0]);
		int numGridCols = Integer.parseInt(args[1]);
		int iterationsToRun = Integer.parseInt(args[2]);
		String inputFile = args[3];
		EngineType engineToRun = EngineType.valueOf(args[4]);
		
		
		//
		// create Game of Life objects
		//
		
		// specify rules to use
		// specify interconnection framework
		// specify simulation environment
		CellLifeRules rules = new ConwayCellLifeRules();
		CellNeighborhood neighborhood = 
			new ConwayCellNeighborhood(0, numGridCols, 0, numGridRows);
		
		// get file for input
		File cellsFile = new File(inputFile);

		// create timing items
		long startTime, endTime, runTime;

		// create objects, distribute data and perform simulation
		// better start our timer
		startTime = System.nanoTime();
		
		GameOfLifeSimulator simulator = null;
		
		switch (engineToRun) {
			case SEQ:
				List<Cell> livingCells = 
					GameOfLifeFileIO.getLiveCells(cellsFile);
				simulator = new SequentialGameOfLifeSimulator(
						livingCells,
						rules,
						neighborhood);
				break;
			case MP: 
				simulator = new NeighborCommGameOfLifeSimulator(
						rules,
						neighborhood,
						commWorld,
						cellsFile);
				break;
			case DYN_MP: 
				simulator = new DynamicNeighborCommGameOfLifeSimulator(
						rules,
						neighborhood,
						commWorld,
						cellsFile,
						1);
				break;
			case SMP: 
			   simulator = new SMPGameOfLifeSimulator(
                  rules,
                  neighborhood,
                  cellsFile);
				break;
			default:
				List<Cell> liveCells = 
					GameOfLifeFileIO.getLiveCells(cellsFile);
				simulator = new SequentialGameOfLifeSimulator(
						liveCells,
						rules,
						neighborhood);
				break;
		}

		//System.out.printf("%d cells alive\n", simulator.getLivingCellCount());

		// run the simulation
		for (int i = 0; i < iterationsToRun; ++i) {
		   try
		   {
		      simulator.performSimulation();
		   }
		   catch (Exception e)
		   {
		      System.out.println("PerformSimulation Exception: " + e.getMessage());
		   }
			//System.out.printf("Iteration %d, %d: %s cells alive\n", i, commWorld.rank(), simulator.getLivingCellCount());
		}	
		
		System.out.printf("%d has %d cells alive\n", commWorld.rank(), simulator.getLivingCellCount());
		
		for (Cell c : simulator.getCurrentState()) {
			//System.out.printf("%d: %s\n", commWorld.rank(), c);
		}
		
		// display running time
		endTime = System.nanoTime();
		runTime = endTime - startTime;
		System.out.printf("Runtime: %dms\n", 
				convertNanoSecondsToMilliseconds(runTime));
		


		
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
		
		try
      {
         simulator.performSimulation();
      }
      catch (Exception e)
      {
         System.out.println("PerformSimulation Exception: " + e.getMessage());
      }
		
		// lets see the result
		livingCells = simulator.getCurrentState();
		for (Cell cell : livingCells) {
			System.out.printf("%s\n", cell);
		}
		System.out.println();
		
		// again!
		try
      {
         simulator.performSimulation();
      }
      catch (Exception e)
      {
         System.out.println("PerformSimulation Exception: " + e.getMessage());
      }
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
	public static void checkUsage(String[] args) {
		if (args.length != GameOfLife.COMMAND_LINE_LENGTH) {
			System.out.println(GameOfLife.USAGE);
			System.exit(1);
		}
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
