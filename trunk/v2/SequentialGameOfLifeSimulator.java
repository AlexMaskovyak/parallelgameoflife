import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * SequentialGameOfLifeSimulator is responsible for storing game logic, storing
 * game-state, and performing the simulation.
 * @author Alex Maskovyak
 *
 */
public class SequentialGameOfLifeSimulator implements GameOfLifeSimulator {

	//
	// Private member variables
	//
	
	// HashMap key: 
	// <cell used strictly for identification, cell with current neighborcount>
	
	// hold live cells
	protected ConcurrentHashMap<Cell, Cell> livingCells;
	// hold potentially alive cells
	protected ConcurrentHashMap<Cell, Cell> gestatingCells;
	
	// hold those cells which ought to be removed from the livingCells area
	protected List<Cell> cellsToRemove;
	
	// additional cell / game rules
	protected CellLifeRules rules;
	protected CellNeighborhood neighborhood;
	
	
	/**
	 * Default constructor.
	 * @param rules Rules to determine a cell's next state.
	 * @param neighborhood Determines cell connectivity.
	 */
	public SequentialGameOfLifeSimulator(
			CellLifeRules rules, 
			CellNeighborhood neighborhood) {
		this(new ArrayList<Cell>(), rules, neighborhood);
	}
	
	/**
	 * Constructor.
	 * @param liveCells List of live cells to manage.
	 * @param rules Rules to determine a cell's next state.
	 * @param neighborhood Determines cell connectivity.
	 */
	public SequentialGameOfLifeSimulator(
			List<Cell> liveCells, 
			CellLifeRules rules, 
			CellNeighborhood neighborhood) 
	{
		// set our internal values
		this.livingCells = new ConcurrentHashMap<Cell, Cell>();
		this.gestatingCells = new ConcurrentHashMap<Cell, Cell>();
		
		this.cellsToRemove = new ArrayList<Cell>();
		
		this.rules = rules;
		this.neighborhood = neighborhood;
		
		// add live cells to the living cell storage area
		this.addLivingCells(liveCells);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#performSimulation()
	 */
	public void performSimulation() throws Exception {
		// Preparation, clean-up, etc.
		this.cleanCellCounts();
		
		// Neighbor creation and neighbor counting
		this.updateCellCounts();

		// Killing and birthing cells area
		this.updateLiveCellList();
	}
	
	/**
	 * Prepares for the simulation by clearing gestating cell storage areas
	 * and zeroing out the neighbor counts.
	 */
	protected void cleanCellCounts() {
		// clear the gestating area for new arrivals
		this.gestatingCells.clear();
		// clear the removal list for new arrivals
		this.cellsToRemove.clear();
		
		// reset the neighbor counts
		Set<Cell> livingCellSet = this.livingCells.keySet();
		for (Cell livingCell : livingCellSet) {
			livingCell.neighborCount = 0;
		}		
	}
	
	/**
    * Creates an up-to-date neighbor count for all live cells as well as their
    * neighbors (which will be created as needed).
    */
   protected void updateCellCounts() {
      updateCellCounts(null);
   }
	
	/**
	 * Creates an up-to-date neighbor count for all live cells as well as their
	 * neighbors (which will be created as needed).
	 */
	protected void updateCellCounts(HashMap<Cell, Cell> localLivingCells) {
		//
		// setup temporary references
		//
	
		Set<Cell> livingCellSet;
		
		if (localLivingCells == null)
		{
		   livingCellSet = this.livingCells.keySet();
		}
		else
		{
		   livingCellSet = localLivingCells.keySet();
		}
		
		List<Cell> neighbors;

		// add neighbors to the gestating area where needed
		for (Cell livingCell : livingCellSet) {
			neighbors = this.neighborhood.getNeighborIdentities(livingCell);
			
			// go through the newbies
			for (Cell neighbor : neighbors) {
				// check to see if it is alive
				Cell liveCell = this.livingCells.get(neighbor);
				
				// if it is already alive, increase its count
				if (liveCell != null) {
					liveCell.neighborCount++;
					continue;
				}
				
				// determine whether it is gestating
				Cell gestatingCell = this.gestatingCells.get(neighbor);
				// if it is already there, update its count
				if (gestatingCell != null) {
					gestatingCell.neighborCount++;
					continue;
				}
				
				// we have to create our new potential life that ought to be
				// gestating!
				neighbor.neighborCount = 1;
				this.gestatingCells.put(neighbor, neighbor);
			}
		}		
	}

	/**
	 * Performs killing and birthing of cells.  Updates the live cell lists
	 * with new arrivals.
	 */
	protected void updateLiveCellList() {
		//
		// setup temporary references
		//
		Set<Cell> livingCellSet = this.livingCells.keySet();
		
		// for the living cells, determine whether they remain alive
		for (Cell livingCell : livingCellSet) {
			if (!rules.lives(livingCell.neighborCount)) {
				this.cellsToRemove.add(livingCell);
			}
		}
		
		// remove those that fail the life test
		for (Cell toRemove : this.cellsToRemove) {
			this.livingCells.remove(toRemove);
		}
		
		// for gestating cells, determine whether they should be birthed
		for (Cell gestatingCell : this.gestatingCells.keySet()) {
			if (rules.isBorn(gestatingCell.neighborCount)) {
				// add them to the living cell list
				this.livingCells.put(gestatingCell, gestatingCell);
			}
		}		
	}
	
	/* 
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#getCurrentState()
	 */
	public List<Cell> getCurrentState() {
		List<Cell> cells = new ArrayList<Cell>(this.livingCells.keySet());
		return cells;
	}

	/* 
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#addLivingCell()
	 */	
	public void addLivingCell(Cell livingCell) {
		// ensure that this cell is a resident of the neighborhood
		// if it isn't, don't bother with the needless computation
		if (this.neighborhood.isResident(livingCell) && !this.livingCells.containsKey(livingCell)) {
			this.livingCells.put(livingCell, livingCell);			
		}
	}

	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#addLivingCells()
	 */	
	public void addLivingCells(List<Cell> livingCells) {
		for (Cell c : livingCells) {
			this.addLivingCell(c);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#clearLivingCells()
	 */
	public void clearLivingCells() {
		this.livingCells.clear();
		this.gestatingCells.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#removeLivingCell(Cell)
	 */
	public void removeLivingCell(Cell livingCell) {
		this.livingCells.remove(livingCell);
	}

	/*
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#removeLivingCells(java.util.List)
	 */
	public void removeLivingCells(List<Cell> livingCells) {
		for (Cell c : livingCells) {
			this.livingCells.remove(c);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see GameOfLifeSimulator#getLivingCellCount()
	 */
	public int getLivingCellCount() {
		return this.livingCells.size();
	}
}
