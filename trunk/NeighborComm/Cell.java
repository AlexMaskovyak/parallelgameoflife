
/**
 * A petite Cell class that basically functions to preserve Cell identity
 * for hashing and comparison purposes.
 * It also stores the quantity of neighbors it possesses.
 * @author Alex Maskovyak
 *
 */
public class Cell implements java.io.Serializable {

	public final int x, y;
	public int neighborCount;
	public boolean isBorderCell;
	private static final int HASHFACTOR = 65521;
	
	/**
	 * Default constructor.
	 * @param x X coordinate value.
	 * @param y Y coordinate value.
	 */
	public Cell(int x, int y) {
		this.x = x;
		this.y = y;
		this.isBorderCell = false;
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
}
