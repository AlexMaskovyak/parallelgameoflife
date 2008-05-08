import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Iterator;

import edu.rit.mp.BooleanBuf;
import edu.rit.pj.CommRequest;
import edu.rit.pj.CommStatus;
import edu.rit.util.Range;
import edu.rit.pj.Comm;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.IntegerBuf;
import edu.rit.util.Arrays;


/**
 * MasterCommGameOfLifeSimulator is responsible for storing game logic, storing
 * game-state, and performing the simulation.
 * @author Sean Janis 
 * @author Alex Maskovyak
 *
 */
public class NeighborCommGameOfLifeSimulator extends SequentialGameOfLifeSimulator {
	//
	// Private member variables
	//
	
	// constants
	protected static final int MASTERRANK = 0;

	protected Comm commWorld;
	protected int numProcessors;
	protected int numberOfWorkers;

	protected int processorRank;

	// border information
	protected int leftProcessorRank;
	protected int rightProcessorRank;
	protected int leftBorderYBound;
	protected int rightBorderYBound;

	// hold border cells
	protected HashMap<Cell, Cell> borderCells;
	protected List<Cell> leftBorder;
	protected List<Cell> rightBorder;

	// border cell message passing
	public static final int LEFT_BORDER_TAG = 0;
	public static final int RIGHT_BORDER_TAG = 1;

	// control message passing size
	protected int largestSliceToReceive;

   

	
	/**
	 * Constructor.
	 * @param liveCells List of live cells to manage.
	 * @param rules Rules to determine a cell's next state.
	 * @param neighborhood Determines cell connectivity.
	 */
	public NeighborCommGameOfLifeSimulator(
			CellLifeRules rules, 
			CellNeighborhood neighborhood,
			Comm commWorld,
			File cellsFile)	
	{
		// call superconstructor
		super(new ArrayList<Cell>(), rules, neighborhood);

		
//		System.out.println("Done superconstructing.");
		
		// set communication values
		this.commWorld = commWorld;
		this.processorRank = this.commWorld.rank();
		this.numProcessors = this.commWorld.size();
		this.numberOfWorkers -= 1;
		
//		System.out.println("Done with commworld");

		
		// set border stores
		this.borderCells = new HashMap<Cell, Cell>();
		this.leftBorder = new ArrayList<Cell>(this.largestSliceToReceive);
		this.rightBorder = new ArrayList<Cell>(this.largestSliceToReceive);

//		System.out.println("Done with border storage");
		
		// set border information
		this.leftBorderYBound = Integer.MAX_VALUE;
		this.rightBorderYBound = Integer.MIN_VALUE;
		
		// control receive/send buffers
		this.largestSliceToReceive = 
			((int)this.neighborhood.getBounds().getMaxX() * (int)this.neighborhood.getBounds().getMaxY()) / this.numProcessors;
      
//		System.out.println("Done calculating largest slice");
		
		// determine neighbor ranks
		this.assignNeighborBorderIDs();

//		System.out.printf("%d Going to start distribution\n", this.processorRank);
		
		// distribute information
		if (this.processorRank == this.MASTERRANK) {
			List<Cell> livingCells = GameOfLifeFileIO.getLiveCells(cellsFile);
//			System.out.printf("Master just got the living cells : %d\n", livingCells.size());
			this.distributeDataToWorkers(livingCells);
		}
		else {
			this.receiveDataFromMaster();
		}
	}
   
	
	/** 
	 * Master Processor method to send Living Cell Slices to each available 
	 * Worker Processor.
	 * @param livingCells List of cells to distribute.
	 */
	public void distributeDataToWorkers(List<Cell> livingCells) {
		System.out.println("Sending to workers");
		Range[] colRanges = new Range(0, livingCells.size()-1).subranges(this.numProcessors);
		Cell[] livingCellHolder = livingCells.toArray(new Cell[livingCells.size()]);
            
		ObjectBuf<Cell>[] livingCellSlices = ObjectBuf.sliceBuffers(livingCellHolder, colRanges);
		CommRequest request = new CommRequest();
      
		for (int nDestProcessorRank = 1; nDestProcessorRank < this.numProcessors; nDestProcessorRank++) {
			try {
				this.commWorld.send(nDestProcessorRank, livingCellSlices[nDestProcessorRank], request);
			}
			catch (IOException e) {
				System.out.println("Could not send to Processor: " + nDestProcessorRank);
			}
		}
        
	   //
	   // Add First Slice to Master's Simulation Hash Map so that it
	   // can also perform computations.
	   //
    
	   for (int i = 0, length = livingCellSlices[this.MASTERRANK].length(); i < length; ++i) {
		   Cell c = livingCellSlices[this.MASTERRANK].get(i);
//		   System.out.printf("Master getting %s\n", c);
		   this.updateBorderBounds(c);
		   this.addLivingCell(c);
	   }
	   
	   System.out.printf("Master has %d cells\n", this.getLivingCellCount());
	}
   
   /**
    * Worker Processor method to receive Living Cell Slices
    * from the Master Processor.
    */
	public void receiveDataFromMaster()
	{
		//
		// Only allocate for the largest possible slice.
		//
      
		Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
      
		//
		// Wait for data from the Master before proceeding.
		//

		ObjectBuf<Cell> livingCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
		CommStatus receiveStatus = null;
      
		try {
			System.out.println("REceiving");
			receiveStatus = this.commWorld.receive(0, livingCellsToReceive);
			//System.out.println("Processor: " + this.myProcessorRank + " received data of size " + receiveStatus.length);
         
			//
			// Parse the buffer and insert the cells into a local Hash Map
			// for simulation. Also, track which the min and max columns that this
			// Worker receives as this information will be needed for border exchanging.
			//
         
			List<Cell> livingCells = new ArrayList<Cell>();
			
			for (int i = 0; i < receiveStatus.length; i++) {
				Cell currentCell = livingCellsToReceive.get(i);
//				System.out.printf("%d getting cell %s\n", this.processorRank, currentCell);
				this.updateBorderBounds(currentCell);
				this.addLivingCell(currentCell);
			}
			
//			System.out.printf("%d has this many live cells: %d\n", this.processorRank, super.getLivingCellCount());
		}
		catch (IOException e) {
			System.out.println("Processor: " + this.processorRank + " could not receive data");
		}
	}
   
	/**
	 * At each iteration, the Worker Processor will send its 'Border' Cells
	 * to its designated neighbors.
	 */
	public void sendBorders() {
		this.updateBorderCellLists();
		
		Cell[] livingLeftBorderCellHolder = this.leftBorder.toArray(new Cell[this.leftBorder.size()]);
		Cell[] livingRightBorderCellHolder = this.rightBorder.toArray(new Cell[this.rightBorder.size()]);
      
		//
		// Send the Buffers to Neighboring Processors
		//
      
//		for (Cell c : livingLeftBorderCellHolder ) {
//			System.out.printf("%d left border : %s\n", this.processorRank, c);
//		}
//		for (Cell c : livingRightBorderCellHolder) {
//			System.out.printf("%d right border : %s\n", this.processorRank, c);			
//		}
		
		if (this.leftProcessorRank != -1) {
			System.out.printf("%d sending to left processor: %d\n", this.processorRank, this.leftProcessorRank);
			ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingLeftBorderCellHolder);
			CommRequest request = new CommRequest();
         
			
			
			try {
				this.commWorld.send(
						this.leftProcessorRank, 
						LEFT_BORDER_TAG, 
						livingBorderCellSlices,
						request);
            
				//System.out.println("Processor " + this.myProcessorRank + " SENT TO LEFT: " + this.leftProcessorRank);
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
						"send Left Border to Processor: " + this.leftProcessorRank);
			}
		}
      
		if (this.rightProcessorRank != -1) {
			System.out.printf("%d sending to right processor: %d\n", this.processorRank, this.rightProcessorRank);
			ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingRightBorderCellHolder);
			CommRequest request = new CommRequest();
         
			try {
				this.commWorld.send(
						this.rightProcessorRank, 
						RIGHT_BORDER_TAG, 
						livingBorderCellSlices,
						request);
            
				//System.out.println("Processor " + this.myProcessorRank + " SENT TO RIGHT: " + this.rightProcessorRank);
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
                  "send Right Border to Processor: " + this.rightProcessorRank);
            }
		}
	}
   
	/**
	 * At each iteration, the Worker Processor will receive its Border Cells
	 * from its designated neighbors. 
	 * 
	 * NOTE: When receiving, Worker Processors will receive Right Borders from 
	 * their Left Neighbors and Left Borders from their Right Neighbors.
	 */ 
	public void receiveBorders() {
		if (this.rightProcessorRank != -1) {
			System.out.printf("%d receiving from right processor: %d\n", this.processorRank, this.rightProcessorRank);

			//
			// Only allocate for the largest possible slice.
			//
         
			Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
         
			ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
			CommStatus receiveStatus = null;
         			
			try {
				receiveStatus = this.commWorld.receive(
						this.rightProcessorRank,
						LEFT_BORDER_TAG,
						livingBorderCellsToReceive);
            
				//
				// Parse the buffer and insert the cells into a local Hash Map
				// for simulation.
				//
            
				for (int i = 0; i < receiveStatus.length; i++) {
					Cell currentCell = livingBorderCellsToReceive.get(i);
					System.out.printf("%d Received: %s\n", this.processorRank, currentCell);
					if (currentCell != null) {
						this.borderCells.put(currentCell, currentCell);
						this.addLivingCell(currentCell);
					}
				}
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
                  "receive Left Border from Processor: " + this.leftProcessorRank);
			}	
		}
		
		if (this.leftProcessorRank != -1) {
			System.out.printf("%d receiving from left processor: %d\n", this.processorRank, this.leftProcessorRank);
			//
			// Only allocate for the largest possible slice.
			//
         
			Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
         
			ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
			CommStatus receiveStatus = null;
         			
			try {
				receiveStatus = this.commWorld.receive(
						this.leftProcessorRank,
						RIGHT_BORDER_TAG,
						livingBorderCellsToReceive);
            
				//
				// Parse the buffer and insert the cells into a local Hash Map
				// for simulation.
				//
            
				for (int i = 0; i < receiveStatus.length; i++) {
					Cell currentCell = livingBorderCellsToReceive.get(i);
					System.out.printf("%d Received: %s\n", this.processorRank, currentCell);
					if (currentCell != null) {
						this.borderCells.put(currentCell, currentCell);
						this.addLivingCell(currentCell);
					}
				}
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
                  "receive Left Border from Processor: " + this.leftProcessorRank);
			}	
		}
	}

	/*public void sendReceiveLiveCellCount() {
		// prepare border information to send
		this.updateBorderCellLists();
		
		// create temporary arrays
		Cell[] livingLeftBorderCellHolder = this.leftBorder.toArray(new Cell[this.leftBorder.size()]);
		Cell[] livingRightBorderCellHolder = this.rightBorder.toArray(new Cell[this.leftBorder.size()]);
		
		// create send buffers
		ObjectBuf<Cell> leftBorderCellsToSend = ObjectBuf.buffer(livingLeftBorderCellHolder);
		ObjectBuf<Cell> rightBorderCellsToSend = ObjectBuf.buffer(livingRightBorderCellHolder);
      
		// prepare buffer to receive
		Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];     
		ObjectBuf<Cell> leftBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
		ObjectBuf<Cell> rightBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
		CommStatus receiveStatus = null;

		if (this.leftProcessorRank != -1) {
			try {
				receiveStatus = this.commWorld.sendReceive(
									this.leftProcessorRank, 
									leftBorderCellsToSend,
									this.rightProcessorRank,
									rightBorderCellsToReceive);
				
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (this.leftProcessorRank != -1) {
			CommRequest request = new CommRequest();
         
			try {
				this.commWorld.send(
						this.leftProcessorRank, 
						LEFT_BORDER_TAG, 
						livingBorderCellSlices,
						request);
            
				//System.out.println("Processor " + this.myProcessorRank + " SENT TO LEFT: " + this.leftProcessorRank);
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
						"send Left Border to Processor: " + this.leftProcessorRank);
			}
		}
      
		if (this.rightProcessorRank != -1) {
			ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingRightBorderCellHolder);
			CommRequest request = new CommRequest();
         
			try {
				this.commWorld.send(
						this.rightProcessorRank, 
						RIGHT_BORDER_TAG, 
						livingBorderCellSlices,
						request);
            
				//System.out.println("Processor " + this.myProcessorRank + " SENT TO RIGHT: " + this.rightProcessorRank);
			}
			catch (IOException e) {
				System.out.println("Processor: " + this.processorRank + " could not " + 
                  "send Right Border to Processor: " + this.rightProcessorRank);
            }
		}
	}
	
	public void sendReceiveBorders() {
		
	}*/
 
	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#performSimulation()
	 */
	public void performSimulation() {
		this.sendBorders();
		this.receiveBorders();

		for (Cell c : this.livingCells.keySet()) {
			System.out.printf("%d has: %s\n", this.processorRank, c);
		}
		
		super.performSimulation();

		System.out.printf("Post sim %d has cell count: %d\n", this.processorRank, this.getLivingCellCount());
		for (Cell c : this.livingCells.keySet()) {
			System.out.printf("Post sim %d has: %s\n", this.processorRank, c);
		}
		
		System.out.printf("%d : has %d border cells\n", this.processorRank, this.borderCells.size());
		
		// clear the border cells
		for (Cell c : this.borderCells.keySet()) {
			super.livingCells.remove(c);
		}
		this.borderCells.clear();
		
		for (Cell c : this.livingCells.keySet()) {
			if (c.y < this.leftBorderYBound || c.y > this.rightBorderYBound) {
				this.cellsToRemove.add(c);
			}
		}
		
		for (Cell c : this.cellsToRemove) {
			this.livingCells.remove(c);
		}
		
	}

   	/**
   	 * Retrieves this Processor's Rank relative to the PJ Comm World.
   	 */
   	public int getProcessorRank() {
	   	return this.processorRank;
   	}
   
   	/**
   	 * Retrieves the number of processors?
   	 */
   	public int getNumProcessors() {
   		return this.numProcessors;
   	}

   	
   	/**
   	 * Determine this processor's neighbors.  Side effects: defines 
   	 * leftProcessorRank and rightProcessorRank.
   	 */
   	private void assignNeighborBorderIDs() {
   		// compute left neighbor
		if (this.processorRank - 1 == -1) {
			// No Wrapping for now, so make leftmost processor's left neighbor invalid.
			this.leftProcessorRank = -1;
		}	
		else {
			this.leftProcessorRank = (this.processorRank - 1) % this.numProcessors;
		}
      
		// compute right neighbor
		if (this.processorRank + 1 == this.numProcessors) {
			// No Wrapping for now, so make rightmost processor's right neighbor invalid.
			this.rightProcessorRank = -1;
		}
		else {
			this.rightProcessorRank = (this.processorRank + 1) % this.numProcessors;
		}
   	}
   	
   	/**
   	 * Updates the object's borders to contain the newly received cell.
   	 * @param c Cell that possibly extends this processor's borders
   	 */
   	private void updateBorderBounds(Cell c) {
   		if (c.y < this.leftBorderYBound) {
   			this.leftBorderYBound = c.y;
   		}
        if (c.y > rightBorderYBound) {
        	this.rightBorderYBound = c.y;
        }
        
        System.out.printf("%d has border: %d %d\n", this.processorRank, this.leftBorderYBound, this.rightBorderYBound);
   	}
 
   	/**
   	 * Updates the border cell lists for transmission.
   	 */
	private void updateBorderCellLists() {
		//
		// Create left and right borders to send. Since the leftmost and
		// or rightmost border values may be the same as in other processors,
		// create a (+/- 1) buffer for determining them.
		//
      
		this.leftBorder.clear();
		this.rightBorder.clear();
		
		Set<Cell> cells = super.livingCells.keySet();
		for (Cell currentCell : cells) {
			if ((currentCell.y == this.leftBorderYBound) ||
					(currentCell.y == this.leftBorderYBound + 1))
			{
				this.leftBorder.add(currentCell);
			}
			if ((currentCell.y == this.rightBorderYBound) || 
					(currentCell.y == this.rightBorderYBound - 1)) 
			{
				this.rightBorder.add(currentCell);
			}
		}
	}
}
