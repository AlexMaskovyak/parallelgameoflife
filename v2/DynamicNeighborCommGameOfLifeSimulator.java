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
			int tag,
			CommStatus receiveStatus)
	{
		return receiveInteger(neighborRank, tag, receiveStatus);
	}
	
	/**
	 * Receive a single integer value from the specified neighbor using the 
	 * provided message tag.
	 * @param neighborRank Rank of the processor that is to send the provided
	 * 						value.
	 * @param tag Tag this message is associated with.
	 * @param recieveStatus  CommStatus to hold the receive status from the
	 * 							message send.
	 * @return Integer value from the given processor.
	 */
	protected int receiveInteger(
			int neighborRank, 
			int tag, 
			CommStatus receiveStatus) 
	{
		// fail fast
		if (neighborRank == this.NON_NEIGHBOR) {
			return -1;
		}
		
		// create buffer to receive value
		IntegerBuf integerToReceive = IntegerBuf.buffer();
		receiveStatus = null;
			
		try {
			receiveStatus = this.commWorld.receive(
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
	public void performSimulation() {
		this.iterationCount++;
		
		// exchange live cell counts
		List<Cell> myLiveCells = super.getCurrentState();
		Cell[] liveCellArray = myLiveCells.toArray(new Cell[myLiveCells.size()]);
		
		CommStatus receiveStatus = null;
		int myLiveCellCount = super.getLivingCellCount();
		
		// send count values left, get count values from right
		sendLiveCellCount(this.leftProcessorRank, this.LEFT_CELL_COUNT);
		int rightNeighborLiveCellCount = 
			this.receiveLiveCellCount(
				this.rightProcessorRank, 
				this.LEFT_CELL_COUNT, 
				receiveStatus);
		
		// balance out on that side
		// if we are bigger, we need to send to the smaller
		// send adjustments right, get adjustments left
		if (rightNeighborLiveCellCount < (myLiveCellCount * this.thresholdMod)) {
			int toSend = (rightNeighborLiveCellCount - myLiveCellCount) / 2;
			List<Cell> cellsToSend = getRightSliceOfCells(liveCellArray, toSend);
			super.sendLiveCells(this.rightProcessorRank, this.LEFT_CELL_ADJUST, cellsToSend);
			// retract border bounds on right side
			this.rightBorderYBound = liveCellArray[liveCellArray.length - 1 - toSend].y;
		}
		// if we are smaller, we need to receive from the larger
		else if (rightNeighborLiveCellCount > (myLiveCellCount * this.thresholdMod)) {
			ObjectBuf<Cell> received = super.receiveLiveCells(this.leftProcessorRank, this.LEFT_CELL_ADJUST, receiveStatus);
	        for (int i = 0; i < receiveStatus.length; i++) {
	        	Cell currentCell = received.get(i);
	            if (currentCell != null) {
	            	// update borders on left side, expand
	            	this.updateBorderBounds(currentCell);
	            	this.addLivingCell(currentCell);
	            }
	        }
		}
		
		// send values right, get values from left
		myLiveCells = super.getCurrentState();
		liveCellArray = myLiveCells.toArray(new Cell[myLiveCells.size()]);

		sendLiveCellCount(this.rightProcessorRank, this.RIGHT_CELL_COUNT);
		int leftNeighborLiveCellCount = 
			this.receiveLiveCellCount(
					this.leftProcessorRank, 
					this.RIGHT_CELL_COUNT, 
					receiveStatus);
		
		// balance out on that side
		// if we are bigger, we need to send to the smaller
		// send adjustments left, get adjustments right
		if (leftNeighborLiveCellCount < (myLiveCellCount * this.thresholdMod)) {
			int toSend = (leftNeighborLiveCellCount - myLiveCellCount) / 2;
			List<Cell> cellsToSend = getLeftSliceOfCells(liveCellArray, toSend);
			super.sendLiveCells(this.leftProcessorRank, this.RIGHT_CELL_ADJUST, cellsToSend);
			// retract border bounds on left side
			this.leftBorderYBound = liveCellArray[toSend + 1].y;
			//this.leftBorderYBound = liveCellArray[liveCellArray.length - 1].y;
		}
		else if (rightNeighborLiveCellCount > (myLiveCellCount * this.thresholdMod)) {
			ObjectBuf<Cell> received = super.receiveLiveCells(this.rightProcessorRank, this.RIGHT_CELL_ADJUST, receiveStatus);
	        for (int i = 0; i < receiveStatus.length; i++) {
	        	Cell currentCell = received.get(i);
	            if (currentCell != null) {
	            	// expand border bounds on right side
	            	this.updateBorderBounds(currentCell);
	            	this.addLivingCell(currentCell);
	            }
	        }
		}
		
		this.performSimulation();
	}
	
	/**
	 * Get right-portion of the cell array passed in.
	 * @param cells Original cell array.
	 * @param size Number of cells to take from the original array.
	 * @return List of the last n cells of the array.
	 */
	protected List<Cell> getRightSliceOfCells(Cell[] cells, int size) {
		List<Cell> portion = new ArrayList<Cell>();
		
		for (int i = 1; i <= size; ++i) {
			portion.add(cells[cells.length - i]);
		}
		
		return portion;
	}
	
	/**
	 * Get left-portion of the cell array passed in.
	 * @param cells Original cell array.
	 * @param size Number of cells to take from the original array.
	 * @return List of the first n cells of the array.
	 */
	protected List<Cell> getLeftSliceOfCells(Cell[] cells, int size) {
		List<Cell> portion = new ArrayList<Cell>();
		
		for (int i = 0; i < size; ++i) {
			portion.add(cells[i]);
		}
		
		return portion;
	}
}
