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
	 * @param home Cell for which to acquire a neigbhor list.
	 * @return List of cells neighboring the specified cell.
	 */
	public abstract List<Cell> getNeighborIdentities(Cell home);
}
