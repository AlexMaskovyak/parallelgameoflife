import edu.rit.pj.Comm;

/**
 * ParallelGameOfLife is a parallel implementation of the game of life using
 * message passing provided by the Parallel Java API.
 *
 */
public class ParallelGameOfLife {
	
	public static int m_nArguments = 5;
	
	/**
	 * Plays the game of life.
	 * @param args "[# rows] [# columns] [# live cells] [path to input file]"
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// 
		// Initialize variables
		//
		long startTime, endTime, runTime;


		//
		// Initialize Parallel Java Communication Framework.
		//
		Comm.init(args);
		Comm oCommWorld = Comm.world();
		int nNumProcessors = oCommWorld.size();
		int nMyProcessorRank = oCommWorld.rank();


		//
		// Collect command line parameters.
		//
		if (args.length != ParallelGameOfLife.m_nArguments) {
			ParallelGameOfLife.displayUsage();
			System.exit(1);
		}
		int nNumGridRows = Integer.parseInt(args[0]);
		int nNumGridCols = Integer.parseInt(args[1]);
		int nNumAliveCells = Integer.parseInt(args[2]);
		int nNumIterationsToRun = Integer.parseInt(args[3]);
		String strInputFilename = args[4];
     

		//
		// Collect the time at the start of the calculations
		//
		startTime = System.nanoTime();

		
		//
		// Master
		//
		if (nMyProcessorRank == 0) {
			MasterProcessor master = new MasterProcessor(
				nMyProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols, 
				nNumAliveCells, nNumIterationsToRun, strInputFilename);
			master.DistributeDataToWorkers();
			master.DoWork(nNumIterationsToRun);
         
			//
			// Collect the ending time for the calculations and display
			//
			endTime = System.nanoTime();
			runTime = endTime - startTime;
			System.out.format(
				"%d milliseconds elapsed\n", 
				ParallelGameOfLife.convertNanoSecondsToMilliseconds(runTime));
		}
      
		//
		// Worker
		//
		else {
			WorkerProcessor worker = new WorkerProcessor(
				nMyProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols);
			worker.ReceiveDataFromMaster();
			worker.DoWork(nNumIterationsToRun);
		}
	}
	
	/**
	 * Displays the parameter requirements for running this program.
	 */
	public static void displayUsage() {
		System.out.println("java -Dpj.np=[# processes] " + 
				"[# rows] " +
				"[# columns] " +
				"[# cells]" +
				"[path to input file]");
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
