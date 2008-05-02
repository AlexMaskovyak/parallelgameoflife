import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;



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
	private HashMap<Cell, Cell> livingCells;
	// hold potentially alive cells
	private HashMap<Cell, Cell> gestatingCells;
	
	// hold those cells which ought to be removed from the livingCells area
	private List<Cell> cellsToRemove;
	
	// additional cell / game rules
	private CellLifeRules rules;
	private CellNeighborhood neighborhood;
	
	
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
		this.livingCells = new HashMap<Cell, Cell>();
		this.gestatingCells = new HashMap<Cell, Cell>();
		
		this.cellsToRemove = new ArrayList<Cell>();
		
		this.rules = rules;
		this.neighborhood = neighborhood;
		
		// add live cells to the living cell storage area
		this.addLivingCells(liveCells);
	}
	
	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#performSimulation()
	 */
	public void performSimulation() {
		//
		// setup temporary references
		//
		
		List<Cell> neighbors;
		
		
		//
		// Preparation, clean-up, etc.
		//
		
		// clear the gestating area for new arrivals
		this.gestatingCells.clear();
		// clear the removal list for new arrivals
		this.cellsToRemove.clear();
		
		// reset the neighbor counts
		Set<Cell> livingCellSet = this.livingCells.keySet();
		for (Cell livingCell : livingCellSet) {
			livingCell.neighborCount = 0;
		}
		
		
		//
		// Neighbor creation and neighbor counting
		//
		
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
		
		
		//
		// Killing and birthing cells area
		//
		
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
	
	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#getCurrentState()
	 */
	public List<Cell> getCurrentState() {
		List<Cell> cells = new ArrayList<Cell>(this.livingCells.keySet());
		return cells;
	}

	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#addLivingCell()
	 */	
	public void addLivingCell(Cell livingCell) {
		this.livingCells.put(livingCell, livingCell);
	}

	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#addLivingCells()
	 */	
	public void addLivingCells(List<Cell> livingCells) {
		for (Cell c : livingCells) {
			this.livingCells.put(c, c);
		}
	}
}
