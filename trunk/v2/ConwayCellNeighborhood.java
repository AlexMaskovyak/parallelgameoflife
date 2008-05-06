import java.awt.Rectangle;
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
	// private int xMinBounds, xMaxBounds, yMinBounds, yMaxBounds;
	protected boolean hasBounds = false;

	protected Rectangle bounds = null;
	
	/**
	 * Default constructor. Game world has no boundaries.
	 */
	public ConwayCellNeighborhood() {} 
	
	/**
	 * Constructor, creates a boundary for the cells in the game world.
	 * @param xMinBounds Smallest x axis value.
	 * @param xMaxBounds Largest x axis value.
	 * @param yMinBounds Smallest y axis value.
	 * @param yMaxBounds Largest y axis value.
	 */
	public ConwayCellNeighborhood(
			int xMinBounds, 
			int xMaxBounds,
			int yMinBounds,
			int yMaxBounds) 
	{
		this(new Rectangle(xMinBounds, yMinBounds, xMaxBounds - xMinBounds, yMaxBounds - yMinBounds));
	}
	
	/**
	 * Constructor, creates a boundary for the cells in the game world from the
	 * specified bounds.
	 * @param bounds Boundary for this game world.
	 */
	public ConwayCellNeighborhood(Rectangle bounds) {
		this.bounds = bounds;
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
				
				Cell temp = new Cell(xNew, yNew);
				
				// if bounds checking is on, make sure we're within the game
				// world, skip if we're outside
				if (this.isResident(temp)) {
					// throw it away
					continue;
				}
				
				// add life to our neighborhood
				neighborList.add(temp);
			}
		}
		
		return neighborList;
	}

	/* (non-Javadoc)
	 * @see CellNeighborhood#isResident()
	 */	
	public boolean isResident(Cell cell) {
		return ( !this.hasBounds || 
				(this.hasBounds && this.bounds.contains(cell.x, cell.y)) );
	}

	/*
	 * (non-Javadoc)
	 * @see CellNeighborhood#getBounds()
	 */
	public Rectangle getBounds() {
		return this.bounds;
	}
}
