import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.rit.mp.IntegerBuf;
import edu.rit.mp.ObjectBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.CommRequest;
import edu.rit.pj.CommStatus;


/**
 * Simulator extends the basic neighbor communication framework to allow for 
 * dynamic redistribution of live cell processing.  Neighboring processors 
 * periodically relay the total number of cells they are working on to each
 * other.  If the processor finds that it is overworked, it will redistribute
 * a portion of its live cells to its nieghbor.
 * @author Alex Maskovyak
 *
 */
public class DynamicNeighborCommGameOfLifeSimulator extends
		NeighborCommGameOfLifeSimulator {

	protected final static int LEFT_CELL_ADJUST = 50;
	protected final static int RIGHT_CELL_ADJUST = 51;
	protected final static int LEFT_CELL_COUNT = 52;
	protected final static int RIGHT_CELL_COUNT = 53;
	
	protected double thresholdMod = 1.0;
	
	protected int iterationCount;
	protected int iterationThreshold;
	
	/**
	 * Default constructor.
	 * @param rules Cell life rules to follow for determine life/death.
	 * @param neighborhood Connectivity framework for cells to determine neighbors.
	 * @param commWorld Parallel Java communication world.
	 * @param cellsFile File containing cells for simulation.
	 * @param iterationThreshold Number of iterations to pursue before a load
	 * 			readjustment should occur.
	 */
	public DynamicNeighborCommGameOfLifeSimulator(
			CellLifeRules rules, 
			CellNeighborhood neighborhood,
			Comm commWorld,
			File cellsFile)	
	{
		this(rules, neighborhood, commWorld, cellsFile, 10);
	}
	
	/**
	 * Default constructor.
	 * @param rules Cell life rules to follow for determine life/death.
	 * @param neighborhood Connectivity framework for cells to determine neighbors.
	 * @param commWorld Parallel Java communication world.
	 * @param cellsFile File containing cells for simulation.
	 * @param iterationThreshold Number of iterations to pursue before a load
	 * 			readjustment should occur.
	 */
	public DynamicNeighborCommGameOfLifeSimulator(
			CellLifeRules rules, 
			CellNeighborhood neighborhood,
			Comm commWorld,
			File cellsFile,
			int iterationThreshold)	
	{
		super(rules, neighborhood, commWorld, cellsFile);
		
		this.iterationCount = 0;
		this.iterationThreshold = iterationThreshold;
	}
	
	/**
	 * Send this simulator current live cell count to the specified neighbor.
	 * @param neighborRank Rank of the processor that is to receive the
	 * 						live cell count.
	 * @param tag Tag for this message.
	 */
	protected void sendLiveCellCount(int neighborRank, int tag) {
		sendInteger(neighborRank, tag, super.getLivingCellCount());
	}
	
	/**
	 * Send the specified integer value to the specified neighbor with the
	 * given tag.
	 * @param neighborRank Rank of the processors that is to receive the
	 * 						provided value.
	 * @param tag Tag for this message.
	 * @param value Integer to send.
	 */
	protected void sendInteger(int neighborRank, int tag, int value) {
		// fail fast
		if (neighborRank == this.NON_NEIGHBOR) {
			return;
		}

		IntegerBuf liveCellBuf = IntegerBuf.buffer(value);
		CommRequest request = new CommRequest();

		try {
			this.commWorld.send(
					neighborRank, 
					tag, 
					liveCellBuf,
					request);
	            
		}
		catch (IOException e) {
			System.out.println("Processor: " + this.processorRank + " could not " + 
					"send Left Border to Processor: " + this.leftProcessorRank);
		}

	}
	
	/**
	 * Retrieve the live cell count from the specified processor.
	 * @param neighborRank Rank of the processor to receive from.
	 * @param receiveStatus CommStatus to hold the receive status from the
	 * 							message sent.
	 * @return Live cell count from the given processor.
	 */
	protected int receiveLiveCellCount(
			int neighborRank,
			int tag)
	{
		return receiveInteger(neighborRank, tag);
	}
	
	/**
	 * Receive a single integer value from the specified neighbor using the 
	 * provided message tag.
	 * @param neighborRank Rank of the processor that is to send the provided
	 * 						value.
	 * @param tag Tag this message is associated with.
	 * @return Integer value from the given processor.
	 */
	protected int receiveInteger(
			int neighborRank, 
			int tag) 
	{
		// fail fast
		if (neighborRank == this.NON_NEIGHBOR) {
			return -1;
		}
		
		// create buffer to receive value
		IntegerBuf integerToReceive = IntegerBuf.buffer();
		super.receiveStatus = null;
			
		try {
			super.receiveStatus = this.commWorld.receive(
					neighborRank, 
					tag,
					integerToReceive);
		}
		catch (Exception e) {
            System.out.printf(
            		"Processor: %d could not receive Left Border from Processor: %d\n", 
            		this.processorRank,this.leftProcessorRank);
			e.printStackTrace();
			return -1;
		}
		
		return integerToReceive.get(0);
	}

	/*
	 * (non-Javadoc)
	 * @see NeighborCommGameOfLifeSimulator#performSimulation()
	 */
	public void performSimulation() throws Exception {
		this.iterationCount++;
	
		
		if ((this.iterationCount % this.iterationThreshold) == 0) {
			performLoadBalancing();
		}
		
		/*System.out.printf("Load balanced: %d: left border=%d, right border=%d\n",
				this.processorRank, 
				super.leftBorderYBound,
				super.rightBorderYBound);*/
		
		super.performSimulation();
		
	}
	
	/**
	 * Causes neighbors to undergo cell swapping to balance loads.
	 */
	protected void performLoadBalancing() {
		
		
		// prepare information for dissemination
		List<Cell> myLiveCells = super.getCurrentState();
		Cell[] liveCellArray = myLiveCells.toArray(new Cell[myLiveCells.size()]);
		java.util.Arrays.sort(liveCellArray, Cell.RowComparator);
		
		int myCellCount = super.getLivingCellCount();

		if (this.processorRank % 2 == 0) {
			sendLiveCellCount(this.rightProcessorRank, this.LEFT_CELL_COUNT);
			int neighborCellCount = 
				receiveLiveCellCount(
					this.rightProcessorRank, 
					this.RIGHT_CELL_COUNT);
			
			// determine whether sending should occur and in which direction
			
			// receive as the left processor
			if ((neighborCellCount > -1 ) && neighborCellCount > (myCellCount * this.thresholdMod)) {
				/*System.out.printf("Balancing: %d: I am receiving from my right, %d\n", 
						this.processorRank, this.rightProcessorRank);*/
				
				int toReceive = (neighborCellCount - myCellCount) / 2;
				
				if (toReceive == 0) {
					return;
				}
				
				ObjectBuf<Cell> received = 
					super.receiveLiveCells(this.rightProcessorRank, this.RIGHT_CELL_ADJUST);
		        
				for (int i = 0; i < super.receiveStatus.length; i++) {
		        	Cell currentCell = received.get(i);
		            if (currentCell != null) {
		            	// update borders on left side, expand
						/*System.out.printf("%d received from %d: %s\n", 
								this.processorRank, 
								this.rightProcessorRank, 
								currentCell);*/
						
		            	this.updateBorderBounds(currentCell);
		            	this.addLivingCell(currentCell);
		            }
		        }
			}
			
			// send as the left processor
			else if ((neighborCellCount > -1) && (neighborCellCount * this.thresholdMod) < myCellCount) {
				int toSend = (myCellCount - neighborCellCount) / 2;
				
				if (toSend == 0) {
					return;
				}
				
				List<Cell> cellsToSend = getRightSliceOfCells(liveCellArray, toSend);
				
				/*System.out.printf("Balancing: %d: I am sending to my right, %d, %d cells to sendsupposed sent, %d actually\n", 
						this.processorRank, this.rightProcessorRank, toSend, cellsToSend.size());*/

				/*for (Cell c : cellsToSend) {
					System.out.printf("%d sending to %d: %s\n", 
							this.processorRank, 
							this.rightProcessorRank, 
							c);
				}*/
				
				super.sendLiveCells(this.rightProcessorRank, this.LEFT_CELL_ADJUST, cellsToSend);
				// retract border bounds on right side
				this.rightBorderYBound = liveCellArray[liveCellArray.length - 1 - toSend].y;
			}
		}
		else {			
			int neighborCellCount = 
				receiveLiveCellCount(
					this.leftProcessorRank,
					this.LEFT_CELL_COUNT);
			sendLiveCellCount(this.leftProcessorRank, this.RIGHT_CELL_COUNT);
			
			// determine whether sending should occur and in which direction
			
			// receive as the right processor
			if ((neighborCellCount > -1) && neighborCellCount > (myCellCount * this.thresholdMod)) {
				int toReceive = (neighborCellCount - myCellCount) / 2;
				
				if (toReceive == 0) {
					return;
				}
				
				ObjectBuf<Cell> received = 
					super.receiveLiveCells(this.leftProcessorRank, this.LEFT_CELL_ADJUST);

				/*System.out.printf("%d: I am receiving from my left, %d, %d cells\n", 
						this.processorRank, this.leftProcessorRank, received.length());	*/
				
				
				for (int i = 0; i < super.receiveStatus.length; i++) {
		        	Cell currentCell = received.get(i);
		            if (currentCell != null) {
		            	// update borders on left side, expand
						/*System.out.printf("%d received from %d: %s\n", 
								this.processorRank, 
								this.leftProcessorRank, 
								currentCell);*/

		            	
		            	this.updateBorderBounds(currentCell);
		            	this.addLivingCell(currentCell);
		            }
		        }
			}
			
			// send as the right processor
			else if ((neighborCellCount > -1) && (neighborCellCount * this.thresholdMod) < myCellCount) {
				int toSend = (myCellCount - neighborCellCount) / 2;
				
				if (toSend == 0) {
					return;
				}
				
				List<Cell> cellsToSend = getLeftSliceOfCells(liveCellArray, toSend);

				/*System.out.printf("%d: I am sending to my left, %d, %d cells supposedly, %d actual\n", 
						this.processorRank, this.leftProcessorRank, toSend, cellsToSend.size());				

				for (Cell c : cellsToSend) {
					System.out.printf("%d sending to %d: %s\n", 
							this.processorRank, 
							this.leftProcessorRank, 
							c);
				}*/

				
				super.sendLiveCells(this.leftProcessorRank, this.RIGHT_CELL_ADJUST, cellsToSend);
				// retract border bounds on right side
				this.leftBorderYBound = liveCellArray[toSend - 1].y;
			}			
		}
		
	}
	
	
	/**
	 * Perform load balancing, left-centric.
	 */
	/*protected void balanceLeftSide() {
		// exchange live cell counts
		List<Cell> myLiveCells = super.getCurrentState();
		Cell[] liveCellArray = myLiveCells.toArray(new Cell[myLiveCells.size()]);
		
		int myLiveCellCount = super.getLivingCellCount();
		
		System.out.printf("%d: I am sending my left, %d, %d cells\n", 
				this.processorRank, this.leftProcessorRank, myLiveCellCount);
		
		// send count values left, get count values from right
		sendLiveCellCount(this.leftProcessorRank, this.LEFT_CELL_COUNT);
		int rightNeighborLiveCellCount = 
			this.receiveLiveCellCount(
				this.rightProcessorRank, 
				this.LEFT_CELL_COUNT);
		
		// balance out on that side
		// if we are bigger, we need to send to the smaller
		// send adjustments right, get adjustments left
		System.out.printf("%d: My right, %d, has %d cells.\n", 
				this.processorRank, rightProcessorRank, rightNeighborLiveCellCount);
		
		if (rightNeighborLiveCellCount == -1) {
			return;
		}
		
		if ((rightNeighborLiveCellCount * this.thresholdMod) < myLiveCellCount) {
			int toSend = (rightNeighborLiveCellCount - myLiveCellCount) / 2;
			List<Cell> cellsToSend = getRightSliceOfCells(liveCellArray, toSend);
			
			System.out.println();
			
			super.sendLiveCells(this.rightProcessorRank, this.LEFT_CELL_ADJUST, cellsToSend);
			// retract border bounds on right side
			this.rightBorderYBound = liveCellArray[liveCellArray.length - 1 - toSend].y;
		}
		// if we are smaller, we need to receive from the larger
		else if (rightNeighborLiveCellCount > (myLiveCellCount * this.thresholdMod)) {
			ObjectBuf<Cell> received = super.receiveLiveCells(this.leftProcessorRank, this.LEFT_CELL_ADJUST);
	        for (int i = 0; i < super.receiveStatus.length; i++) {
	        	Cell currentCell = received.get(i);
	            if (currentCell != null) {
	            	// update borders on left side, expand
	            	this.updateBorderBounds(currentCell);
	            	this.addLivingCell(currentCell);
	            }
	        }
		}
	}*/

	/**
	 * Perform load balance, right-side centric.
	 */
	/*protected void balanceRightSide() {
		// send values right, get values from left
		List<Cell> myLiveCells = super.getCurrentState();
		Cell[] liveCellArray = myLiveCells.toArray(new Cell[myLiveCells.size()]);

		int myLiveCellCount = super.getLivingCellCount();
		
		sendLiveCellCount(this.rightProcessorRank, this.RIGHT_CELL_COUNT);
		int leftNeighborLiveCellCount = 
			this.receiveLiveCellCount(
					this.leftProcessorRank, 
					this.RIGHT_CELL_COUNT);
	
		if (leftNeighborLiveCellCount == -1) {
			return;
		}
		
		// balance out on that side
		// if we are bigger, we need to send to the smaller
		// send adjustments left, get adjustments right
		if ((leftNeighborLiveCellCount * this.thresholdMod) < myLiveCellCount) {
			int toSend = (leftNeighborLiveCellCount - myLiveCellCount) / 2;
			List<Cell> cellsToSend = getLeftSliceOfCells(liveCellArray, toSend);
			super.sendLiveCells(this.leftProcessorRank, this.RIGHT_CELL_ADJUST, cellsToSend);
			// retract border bounds on left side
			this.leftBorderYBound = liveCellArray[toSend + 1].y;
			//this.leftBorderYBound = liveCellArray[liveCellArray.length - 1].y;
		}
		else if (leftNeighborLiveCellCount > (myLiveCellCount * this.thresholdMod)) {
			ObjectBuf<Cell> received = super.receiveLiveCells(this.rightProcessorRank, this.RIGHT_CELL_ADJUST);
	        for (int i = 0; i < super.receiveStatus.length; i++) {
	        	Cell currentCell = received.get(i);
	            if (currentCell != null) {
	            	// expand border bounds on right side
	            	this.updateBorderBounds(currentCell);
	            	this.addLivingCell(currentCell);
	            }
	        }
		}		
	}*/
	
	/**
	 * Get right-portion of the cell array passed in, and removes them from the
	 * the livelist.
	 * @param cells Original cell array.
	 * @param size Number of cells to take from the original array.
	 * @return List of the last n cells of the array.
	 */
	protected List<Cell> getRightSliceOfCells(Cell[] cells, int size) {
		List<Cell> portion = new ArrayList<Cell>();
		
		for (int i = 1; i <= size; ++i) {
			portion.add(cells[cells.length - i]);
			super.livingCells.remove(cells[cells.length - i]);
		}
		
		return portion;
	}
	
	/**
	 * Get left-portion of the cell array passed in, and removes them from the
	 * livelist.
	 * @param cells Original cell array.
	 * @param size Number of cells to take from the original array.
	 * @return List of the first n cells of the array.
	 */
	protected List<Cell> getLeftSliceOfCells(Cell[] cells, int size) {
		List<Cell> portion = new ArrayList<Cell>();
		
		for (int i = 0; i < size; ++i) {
			portion.add(cells[i]);
			super.livingCells.remove(cells[i]);
		}
		
		return portion;
	}
}
