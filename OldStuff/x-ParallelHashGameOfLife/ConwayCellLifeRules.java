/**
 * Implements John Conway's Game of Life rules for determining whether a given
 * cell with a particular number of live neighbors should undergo a state 
 * change.
 * @author Alex Maskovyak
 *
 */
public class ConwayCellLifeRules implements CellLifeRules {

	private static final int[] neighborsToLive = new int[] {2, 3};
	private static final int[] neighborsForBirth = new int[] {3};
	
	/**
	 * Default constructor.
	 */
	public ConwayCellLifeRules() { }
	
	/* (non-Javadoc)
	 * @see CellLifeRules#isBorn()
	 */	
	public boolean isBorn(int liveNeighbors) {
		for (int i : neighborsForBirth) {
			if (i == liveNeighbors) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see CellLifeRules#lives()
	 */	
	public boolean lives(int liveNeighbors) {
		for (int i : neighborsToLive) {
			if (i == liveNeighbors) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see CellLifeRules#dies()
	 */	
	public boolean dies(int liveNeighbors) {
		return !lives(liveNeighbors);
	}
}
