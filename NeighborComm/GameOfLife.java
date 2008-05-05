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
	 */
	public static void main(String[] args) throws Exception {
	   
	   //
      // Initialize Parallel Java Communication Framework.
      //
      
	   Comm.init(args);
      Comm oCommWorld = Comm.world();
      int numProcessors = oCommWorld.size();
      int myProcessorRank = oCommWorld.rank();
      
		// create Game of Life objects
		// specify which rules to use
		// specify which interconnection framework
		// specify simulation environment
	   
	   int nNumGridRows = Integer.parseInt(args[0]);
      int nNumGridCols = Integer.parseInt(args[1]);
      int nNumLivingCellsInFile = Integer.parseInt(args[2]);
      int nNumIterationsToRun = Integer.parseInt(args[3]);
      String strInputFilename = args[4];
      
      CellLifeRules rules = new ConwayCellLifeRules();
      ConwayCellNeighborhood neighborhood = new ConwayCellNeighborhood(0, nNumGridRows, 0, nNumGridCols);
      NeighborCommGameOfLifeSimulator simulator = 
         new NeighborCommGameOfLifeSimulator(
               rules, 
               neighborhood, 
               nNumLivingCellsInFile, 
               myProcessorRank, 
               numProcessors);
      
      // run the glider simulation as an example
      long startTime = System.nanoTime();
      
      GameOfLife.runNeighborCommTestCase(simulator, nNumIterationsToRun, strInputFilename);
      
      // display running time
      long endTime = System.nanoTime();
      long runTime = endTime - startTime;
      System.out.printf("Processor %d Runtime: %dms\n", myProcessorRank, convertNanoSecondsToMilliseconds(runTime));
	}

	/**
	 * Demonstration of the simulation on a glider object.  Also shows how
	 * one would go about reading input, simulating, etc.
	 * @param simulator Simulator to perform Game of Life computations.
	 */
	private static void runNeighborCommTestCase(
	      NeighborCommGameOfLifeSimulator simulator,
	      int nNumIterationsToRun,
	      String strInputFilename
	      ) {

	   //
	   // Master
	   //
	   
	   if (simulator.getProcessorRank() == 0)
	   {
	      List<Cell> livingCells = GameOfLifeFileIO.getLiveCells(strInputFilename, 0, 0);
	      simulator.distributeDataToWorkers(livingCells);
	   }
	   
	   //
	   // Worker
	   //
	   
	   else
	   {
	      simulator.receiveDataFromMaster();
	   }
	   
	   //
	   // After data has been distributed, perform the simulation.
	   //
	   
	   for (int i = 0; i < nNumIterationsToRun; i++)
      {
         simulator.sendBorders();
         simulator.receiveBorders();
         simulator.performSimulation();
      }
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
