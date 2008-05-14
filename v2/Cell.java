import java.util.Comparator;


/**
 * A petite cell class that basically functions to preserve Cell identity for
 * hashing and comparison purposes. It also serves as a storage device for
 * neighbor counts.  Additionally, it possesses two comparators for sorting
 * purposes known as RowComparator and ColumnComparator.
 * @author Alex Maskovyak
 * @TODO Determine if there is a way to secure x, y so that this can be
 * 			extended into other Cell objects (3D cell, etc.)
 */
public class Cell implements java.io.Serializable, Comparable<Cell> {

	//
	// public fields
	// 
	public int neighborCount;
	final int x, y;		
	
	//
	// private fields
	//
	private static final int HASHFACTOR = 65521;
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Default constructor.
	 * @param x X coordinate value.
	 * @param y Y coordinate value.
	 */
	public Cell(int x, int y) {
		this.x = x;
		this.y = y;
	}
	

	/**
	 * Implementation for the Comparable interface.
	 */
	public int compareTo(Cell arg0) {
		// sort first by x, then by y if needed
		int xDiff = this.x - arg0.x;
		if (xDiff == 0) {
			return this.y - arg0.y;
		}
		
		return xDiff;
	}
	
	/**
	 * Compares cell objects to determine equivalence.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Cell)) {
			return false;
		}
		return (x == ((Cell)o).x && y == ((Cell)o).y);
	}
	
	/**
	 * Override Java's default hash code.
	 */
	public int hashCode() {
		return (HASHFACTOR * this.x) + this.y;
	}
	
	/**
	 * Override Java's default string code.  Useful for debugging.
	 */
	public String toString() {
		return String.format("Cell at: (%s)", this.toSuccinctString());
	}
	
	/**
	 * Succinct form of writing the cell's information. Useful for outputting
	 * to a file.
	 * @return Returns the cell's string information.
	 */
	public String toSuccinctString() {
		return String.format("%d %d", this.x, this.y);
	}
	
	
	//
	// Class fields.
	//
	
	/**
	 * Anonymous comparison object for cell objects, useful for sorting cells
	 * by column and then row.
	 */
	public static final Comparator<Cell> ColumnComparator = new Comparator<Cell>() {
		public int compare(Cell cell1, Cell cell2) {
			// sort first by y, then by x if needed
			int yDiff = cell1.y - cell2.y;	
			if (yDiff == 0) {
				return cell1.x - cell2.x;
			}
			
			return yDiff;
		}
	};
	
	/**
	 * Anonymous comparison class for cell objects, useful for sorting cells
	 * by row and then column.
	 */
	public static final Comparator<Cell> RowComparator = new Comparator<Cell>() {
		public int compare(Cell cell1, Cell cell2) {
			// sort first by x, then by y if needed
			int xDiff = cell1.x - cell2.x;
			if (xDiff == 0) {
				return cell1.y - cell2.y;
			}
			
			return xDiff;
		}
	};
}
