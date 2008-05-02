import java.util.List;

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
	public static void main(String[] args) {
		// create Game of Life objects
		// specify which rules to use
		// specify which interconnection framework
		// specify simulation environment
		CellLifeRules rules = new ConwayCellLifeRules();
		CellNeighborhood neighborhood = new ConwayCellNeighborhood();
		GameOfLifeSimulator simulator = 
			new SequentialGameOfLifeSimulator(rules, neighborhood);
		
		// create timing items
		long startTime, endTime, runTime;
		
		// run the glider simulation as an example
		startTime = System.nanoTime();
		
		GameOfLife.gliderTestCase(simulator);
		
		// display running time
		endTime = System.nanoTime();
		runTime = endTime - startTime;
		System.out.printf("Runtime: %dms\n", 
				convertNanoSecondsToMilliseconds(runTime));
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
