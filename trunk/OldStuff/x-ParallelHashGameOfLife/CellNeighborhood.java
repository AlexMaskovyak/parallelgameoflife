import java.awt.Rectangle;
import java.util.List;

/**
 * Controls the identities of neighbor cells
 * @author Alex Maskovyak
 *
 */
public interface CellNeighborhood {

	/**
	 * Returns a list of cells whose coordinates correspond to the neighbors
	 * of the specified cell.
	 * @param home Cell for which to acquire a neighbor list.
	 * @return List of cells neighboring the specified cell.
	 */
	public abstract List<Cell> getNeighborIdentities(Cell home);
	
	/**
	 * Determines whether a given cell can be found inside of the neighborhood.
	 * @param cell Cell for which to determine residency.
	 * @return True if the cell can be found in this neighborhood, false 
	 * 			otherwise.
	 */
	public abstract boolean isResident(Cell cell);
	
	/**
	 * Reveals the internal boundary that exist for this neighborhood if any.
	 * @return A rectangle containing the enforced boundary for cells in the 
	 * 			neighborhood, null if no such boundary is being 
	 * 			enforced/exists.
	 */
	public abstract Rectangle getBounds();
}
