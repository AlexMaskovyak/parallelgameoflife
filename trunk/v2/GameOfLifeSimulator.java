import java.util.List;

/**
 * This encapsulates simulator logic.  
 * @author Alex Maskovyak
 *
 */
public interface GameOfLifeSimulator {

	/**
	 * Adds the living cell upon which the simulator will perform next-state 
	 * computations.
	 */
	public abstract void addLivingCell(Cell livingCell);
	
	/**
	 * Adds the list of living cells upon which the simulator will perform
	 * next-state computations.
	 * @param livingCell
	 */
	public abstract void addLivingCells(List<Cell> livingCells);
	
	/**
	 * Clears the living cells stored in the simulator.
	 */
	public abstract void clearLivingCells();
	
	/**
	 * Simulate the cell interactions to produce the next game of life state.
	 */
	public abstract void performSimulation();

	/**
	 * Acquire the current set of live cells from this ZoneManager.
	 * @return The list of live cells contained in this manager.
	 */
	public abstract List<Cell> getCurrentState();

}