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
	 * Acquire the current set of live cells from this ZoneManager.
	 * @return The list of live cells contained in this manager.
	 */
	public abstract List<Cell> getCurrentState();

	/**
	 * Determines how many living cells are present in the simulator.
	 * @return Count of living cells this simulator has in its current state.
	 */
	public abstract int getLivingCellCount();
	
	/**
	 * Simulate the cell interactions to produce the next game of life state.
	 */
	public abstract void performSimulation() throws Exception;

	/**
	 * Removes the living cell stored in this simulator that is equal to the
	 * cell specified.
	 * @param livingCell Cell to remove.
	 */
	public abstract void removeLivingCell(Cell livingCell);
	
	/**
	 * Removes every living cell stored in this simulator that is equal to one
	 * of the specified cells.
	 * @param livingCells Cells to remove.
	 */
	public abstract void removeLivingCells(List<Cell> livingCells);
}