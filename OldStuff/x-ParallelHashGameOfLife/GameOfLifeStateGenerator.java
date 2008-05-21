import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Creates new game of life states.
 * @author Alex Maskovyak
 *
 */
public class GameOfLifeStateGenerator {
	
	public static Random generator;
	
	/**
	 * Default constructor, creates a new generator with a new seed.
	 */
	public GameOfLifeStateGenerator() {
		GameOfLifeStateGenerator.generator = new Random(System.nanoTime());
	}
	
	/**
	 * Re-seeds the random number generator.
	 */
	public void resetGenerator() {
		GameOfLifeStateGenerator.generator.setSeed(System.nanoTime());
	}
	
	/**
	 * Creates a list of a random number of cells randomly positioned in the 
	 * range between: 0...MAX_INT inclusive.
	 * @return List of a random number of randomly positioned cells.
	 */
	public List<Cell> createCellList() {
		int cellsToCreate = getRandomIntInRange(0, Integer.MAX_VALUE);
		return createCellList(cellsToCreate);
	}
	
	/**
	 * Creates a list of the specified number of cells randomly positioned in
	 * the range between: 0...MAX_INT inclusive.
	 * @param cellsToCreate Number of cells to create.
	 * @return List of randomly positioned cells of the number specified.
	 */
	public List<Cell> createCellList(int cellsToCreate) {
		return createCellList(cellsToCreate, 0, Integer.MAX_VALUE);
	}
	
	/**
	 * Creates a list of the specified number of cells randomly positioned in 
	 * the specified positive square range.
	 * @param cellsToCreate Number of cells to create.
	 * @param minCoordinateValue Minimum value in the range, must be positive.
	 * @param maxCoordinateValue Maximum value in the range, must be positive.
	 * @return List of randomly positioned cells of the number specified
	 * 			positioned in the range specified.
	 */
	public List<Cell> createCellList(
			int cellsToCreate, 
			int minCoordinateValue, 
			int maxCoordinateValue) 
		{
		List<Cell> liveCells = new ArrayList<Cell>();
		Cell temp;
		int x, y;
		
		// ensure that we are not attempting to generate more cells than
		// is possible in the given range
		int coordinateValueRange = (maxCoordinateValue - minCoordinateValue);
		int maxCellsPossible = coordinateValueRange * coordinateValueRange;
		
		
		for (int i = 0; i < cellsToCreate && i != maxCellsPossible; ++i) {
			x = this.getRandomIntInRange(minCoordinateValue, maxCoordinateValue);
			y = this.getRandomIntInRange(minCoordinateValue, maxCoordinateValue);
			temp = new Cell(x, y);
			
			if (!liveCells.contains(temp)) {
				liveCells.add(temp);
			}
			else {
				cellsToCreate++;
			}
		}
		
		return liveCells;
	}
	
	/**
	 * Generates a random integer value in the range specified.
	 * @param low Minimum value to generate.
	 * @param high Maximum value to generate.
	 * @return A random integer within the range specified.
	 */
	private int getRandomIntInRange(int low, int high) {
		// acquire range of values to slim down to
		int scale = high - low + 1;
		// generate a value and prod it into the scale
		int result = GameOfLifeStateGenerator.generator.nextInt() % scale;
		// ensure we are not negative
		result = (result < 0) ? (-result) : result;
		// translate the value 
		return (result + low);
	}
 }
