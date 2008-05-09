/**
 * Interface for Game of Life rules for determining whether a given cell with a
 *  particular number of live neighbors should undergo a state change.
 * @author Alex Maskovyak
 *
 */
public interface CellLifeRules {

	/**
	 * Determines whether a cell should be born based upon the number of living
	 * neighbor cells.
	 * @param liveNeighbors
	 * @return True if the cell should be born, false otherwise.
	 */
	public abstract boolean isBorn(int liveNeighbors);
	
	/**
	 * Determines whether a cell should remain alive based upon the number of
	 * living neighbor cells.
	 * @param liveNeighbors
	 * @return True if the cell should remain alive, false otherwise.
	 */
	public abstract boolean lives(int liveNeighbors);

	/**
	 * Determines whether a cell should remain alive based upon the number of
	 * living neighbor cells.
	 * @param liveNeighbors
	 * @return True if the cell should remain alive, false otherwise.
	 */
	public abstract boolean dies(int liveNeighbors);
}
