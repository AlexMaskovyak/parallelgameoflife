import java.util.ArrayList;
import java.util.List;

/**
 * Determines the identities of the cells in a traditional 2-d cell
 * neighborhood as determined used by Conway and other cellular automata
 * theorists.
 * @author Alex Maskovyak
 *
 */
public class ConwayCellNeighborhood implements CellNeighborhood {

	// boundaries of the game world
	private int xMinBounds, xMaxBounds, yMinBounds, yMaxBounds;
	boolean hasBounds = false;
	
	/**
	 * Default constructor. Game world has no boundaries.
	 */
	public ConwayCellNeighborhood() {} 
	
	/**
	 * Constructor, creates a boundary for the cells in the game world.
	 * @param xSize Size of the gameboard's x axis.
	 * @param ySize Size of the gameboard's y axis.
	 */
	public ConwayCellNeighborhood(
			int xMinBounds, 
			int xMaxBounds,
			int yMinBounds,
			int yMaxBounds) 
	{
		this.xMinBounds = xMinBounds;
		this.xMaxBounds = xMaxBounds;
		this.yMinBounds = yMinBounds;
		this.yMaxBounds = yMaxBounds;
		this.hasBounds = true;
	}
	
	/* (non-Javadoc)
	 * @see CellNeighborhood#getNeighborIdentities()
	 */	
	public List<Cell> getNeighborIdentities(Cell home) {
		
		List<Cell> neighborList = new ArrayList<Cell>();
		
		// current cell's coordinates
		int x = home.x;
		int y = home.y;
		
		// new cell's coordinates
		int xNew, yNew;
		
		// get cell's 8 neighbors
		for (int xMod = -1; xMod <= 1; ++xMod) {
			for (int yMod = -1; yMod <= 1; ++yMod) {
				
				// skip identical coordinate values
				if (xMod == 0 && yMod == 0) {
					continue;
				}
				
				// determine new coordinates
				xNew = x + xMod;
				yNew = y + yMod;
				
				// if bounds checking is on, make sure we're within the game
				// world, skip if we're outside
				if (this.hasBounds) {
					if (xNew < xMinBounds || xNew > xMaxBounds ||
							yNew < yMinBounds || yNew > yMaxBounds) {
						continue;
					}
				}
				
				neighborList.add(new Cell(xNew, yNew));
			}
		}
		
		return neighborList;
	}
}
