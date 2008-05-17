import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.rit.pj.CommRequest;
import edu.rit.pj.CommStatus;
import edu.rit.util.Range;
import edu.rit.pj.Comm;
import edu.rit.mp.ObjectBuf;


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
	protected static final int NON_NEIGHBOR = -1;

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
	protected CommStatus receiveStatus;
	
   
	
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

		
		// set communication values
		this.commWorld = commWorld;
		this.processorRank = this.commWorld.rank();
		this.numProcessors = this.commWorld.size();
		this.numberOfWorkers -= 1;

		
		// set border stores
		this.borderCells = new HashMap<Cell, Cell>();
		this.leftBorder = new ArrayList<Cell>(this.largestSliceToReceive);
		this.rightBorder = new ArrayList<Cell>(this.largestSliceToReceive);

		// set border information
		this.leftBorderYBound = Integer.MAX_VALUE;
		this.rightBorderYBound = Integer.MIN_VALUE;
		
		// control receive/send buffers
		this.largestSliceToReceive = 
			((int)this.neighborhood.getBounds().getMaxX() * (int)this.neighborhood.getBounds().getMaxY()) / this.numProcessors;
      
		// determine neighbor ranks
		this.assignNeighborBorderIDs();

		// distribute information
		if (this.processorRank == this.MASTERRANK) {
			List<Cell> livingCells = GameOfLifeFileIO.getLiveCells(cellsFile);
			this.distributeDataToWorkers(livingCells);
			this.leftBorderYBound = (int)this.neighborhood.getBounds().getMinY();
		}
		else {
			this.receiveDataFromMaster();
			if (this.processorRank == this.numProcessors - 1) {
				this.rightBorderYBound = (int)this.neighborhood.getBounds().getMaxY();
			}
		}
		
		//System.out.printf("%d has left border %d, right border %d\n", this.processorRank, this.leftBorderYBound, this.rightBorderYBound);
	}
   
	
	/** 
	 * Master Processor method to send Living Cell Slices to each available 
	 * Worker Processor.
	 * @param livingCells List of cells to distribute.
	 */
	public void distributeDataToWorkers(List<Cell> livingCells) {
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
		   this.updateBorderBounds(c);
		   this.addLivingCell(c);
	   }
	   
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
		this.receiveStatus = null;
      
		try {
			this.receiveStatus = this.commWorld.receive(0, livingCellsToReceive);
			//System.out.println("Processor: " + this.myProcessorRank + " received data of size " + receiveStatus.length);
         
			//
			// Parse the buffer and insert the cells into a local Hash Map
			// for simulation. Also, track which the min and max columns that this
			// Worker receives as this information will be needed for border exchanging.
			//
         
			List<Cell> livingCells = new ArrayList<Cell>();
			
			for (int i = 0; i < this.receiveStatus.length; i++) {
				Cell currentCell = livingCellsToReceive.get(i);
				this.updateBorderBounds(currentCell);
				this.addLivingCell(currentCell);
			}
			
		}
		catch (IOException e) {
			System.out.println("Processor: " + this.processorRank + " could not receive data");
		}
	}
	/**
	 * Retrieves a buffer of live cells from the specified neighbor using the 
	 * provided tag.
	 * @param neighborRank Neighbor from which to receive cells.
	 * @param tag Non-zero rank of neighbor from which to receive.
	 * @return Living cells from the provided neighbor.
	 */
	protected ObjectBuf<Cell> receiveLiveCells(
			int neighborRank, 
			int tag) 
	{
		// fail fast
		if (neighborRank == this.NON_NEIGHBOR) {
			return null;
		}
		
		// allocate for the largest possible slice
		Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
			
		ObjectBuf<Cell> livingCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
		this.receiveStatus = null;
		
		try {
			this.receiveStatus = this.commWorld.receive(
					neighborRank, 
					tag,
					livingCellsToReceive);
		}
		catch (Exception e) {
            System.out.printf(
            		"Processor: %d could not receive Left Border from Processor: %d\n", 
            		this.processorRank,this.leftProcessorRank);
			e.printStackTrace();
			return null;
		}
		
		return livingCellsToReceive;
	}
	
	/**
	 * Sends the specified list of live cells to the neighbor with the given
	 * rank.
	 * @param neighborRank Neighbor who is to receive the cells.
	 * @param tag Tag with which to send the cells.
	 * @param liveCells Cells to send.
	 */
	protected void sendLiveCells(int neighborRank, int tag, List<Cell> liveCells) {
		// fail fast
		if (neighborRank == this.NON_NEIGHBOR) {
			return;
		}

		Cell[] livingCellArray = liveCells.toArray(new Cell[liveCells.size()]);

		ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingCellArray);
		CommRequest request = new CommRequest();

		try {
			this.commWorld.send(
					neighborRank, 
					tag, 
					livingBorderCellSlices,
					request);
	            
		}
		catch (IOException e) {
			System.out.println("Processor: " + this.processorRank + " could not " + 
					"send Left Border to Processor: " + this.leftProcessorRank);
		}
	}

	
	/* (non-Javadoc)
	 * @see GameOfLifeSimulator#performSimulation()
	 */
	public void performSimulation() {
		// update left and right border cell lists
		this.updateBorderCellLists();
		
		// send our left border, receive someone else's left border
		this.sendLiveCells(this.leftProcessorRank, LEFT_BORDER_TAG, this.leftBorder);
		
		this.receiveStatus = null;
		ObjectBuf<Cell> borderCells = this.receiveLiveCells(this.rightProcessorRank, LEFT_BORDER_TAG);

		
        for (int i = 0; this.receiveStatus != null && i < this.receiveStatus.length; i++) {
        	Cell currentCell = borderCells.get(i);
            if (currentCell != null) {
            	this.borderCells.put(currentCell, currentCell);
            	this.addLivingCell(currentCell);
            }
        }
		
        // our right border, receive someone else's right border
        this.sendLiveCells(this.rightProcessorRank, RIGHT_BORDER_TAG, this.rightBorder);
        
        this.receiveStatus = null;
        borderCells = this.receiveLiveCells(this.leftProcessorRank, RIGHT_BORDER_TAG);
        
        for (int i = 0; this.receiveStatus != null && i < this.receiveStatus.length; i++) {
        	Cell currentCell = borderCells.get(i);
            if (currentCell != null) {
            	this.borderCells.put(currentCell, currentCell);
            	this.addLivingCell(currentCell);
            }
        }

        // simulate with all information
        super.performSimulation();
	
		// clear the border cells
		for (Cell c : this.borderCells.keySet()) {
			// check to determine whether this is a shared border cell
			// lower ranked processors preferentially keep shared border cells
			
			// basically, we only care if this sits on a right border, if it
			// does that means that the other guy should delete it and we 
			// should spare it
			if (c.y == this.rightBorderYBound) {
				continue;
			}
			if (c.y == this.leftBorderYBound) {
				super.livingCells.remove(c);
			}
			
		}
		this.borderCells.clear();
		
        // clear our removal list.
		this.cellsToRemove.clear();
        

		// remove those cells that are outside our bounds
		// update our bounds if the cell is a part of the neighborhood and there is no neighbor
		// to manage them
		for (Cell c : this.livingCells.keySet()) {
			if (c.y < this.leftBorderYBound) {
				this.cellsToRemove.add(c);
				continue;
			}
			
			if (c.y > this.rightBorderYBound) {
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
			this.leftProcessorRank = NeighborCommGameOfLifeSimulator.NON_NEIGHBOR;
		}	
		else {
			this.leftProcessorRank = (this.processorRank - 1) % this.numProcessors;
		}
      
		// compute right neighbor
		if (this.processorRank + 1 == this.numProcessors) {
			// No Wrapping for now, so make rightmost processor's right neighbor invalid.
			this.rightProcessorRank = NeighborCommGameOfLifeSimulator.NON_NEIGHBOR;
		}
		else {
			this.rightProcessorRank = (this.processorRank + 1) % this.numProcessors;
		}
   	}
   	
   	/**
   	 * Updates the object's borders to contain the newly received cell.
   	 * @param c Cell that possibly extends this processor's borders
   	 */
   	protected void updateBorderBounds(Cell c) {
   		if (c.y < this.leftBorderYBound) {
   			this.leftBorderYBound = c.y;
   		}
   		if (c.y > rightBorderYBound) {
        	this.rightBorderYBound = c.y;
        }
   	}
 
   	/**
   	 * Updates the border cell lists for transmission.
   	 */
	protected void updateBorderCellLists() {
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


/**
 * At each iteration, the Worker Processor will send its 'Border' Cells
 * to its designated neighbors.
 */
/*
public void sendLeftBorder() {
   this.updateBorderCellLists();
   Cell[] livingLeftBorderCellHolder = this.leftBorder.toArray(new Cell[this.leftBorder.size()]);
   
   if (this.leftProcessorRank != -1) {
     ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingLeftBorderCellHolder);
     CommRequest request = new CommRequest();

     try {
        this.commWorld.send(
              this.leftProcessorRank, 
              LEFT_BORDER_TAG, 
              livingBorderCellSlices,
              request);
        
     }
     catch (IOException e) {
        System.out.println("Processor: " + this.processorRank + " could not " + 
              "send Left Border to Processor: " + this.leftProcessorRank);
     }
  }
}*/

/*public void sendRightBorder() {
   this.updateBorderCellLists();
   Cell[] livingRightBorderCellHolder = this.rightBorder.toArray(new Cell[this.rightBorder.size()]);
   
   if (this.rightProcessorRank != -1) {
     //System.out.printf("%d sending to right processor: %d\n", this.processorRank, this.rightProcessorRank);
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
}*/

/**
* At each iteration, the Worker Processor will receive its Border Cells
* from its designated neighbors. 
* 
* NOTE: When receiving, Worker Processors will receive Right Borders from 
* their Left Neighbors and Left Borders from their Right Neighbors.
*/ 
/*
public void receiveLeftBorder() {
   if (this.leftProcessorRank != -1) {
     //System.out.printf("%d receiving from left processor: %d\n", this.processorRank, this.leftProcessorRank);
     //
     // Only allocate for the largest possible slice.
     //
     
     Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
     
     ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
     CommStatus receiveStatus = null;
     
     //
     // Looking for this Processor's Left Border, which is another Processor's
     // Right Border.
     //
     
     try {
        receiveStatus = this.commWorld.receive(
              this.leftProcessorRank,
              RIGHT_BORDER_TAG,
              livingBorderCellsToReceive);
        
        //
        // Parse the buffer and insert the cells into a local Hash Map
        // for simulation.
        //
        
        //System.out.printf("%d Received: %d\n", this.processorRank, receiveStatus.length);
        
        for (int i = 0; i < receiveStatus.length; i++) {
           Cell currentCell = livingBorderCellsToReceive.get(i);
           
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
}*/


/*public void receiveRightBorder() {
   if (this.rightProcessorRank != -1) {
     //System.out.printf("%d receiving from right processor: %d\n", this.processorRank, this.rightProcessorRank);

     //
     // Only allocate for the largest possible slice.
     //
     
     Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
     
     ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
     CommStatus receiveStatus = null;
     
     //
     // Looking for this Processor's Right Border, which is another Processor's
     // Left Border.
     //
     
     try {
        receiveStatus = this.commWorld.receive(
              this.rightProcessorRank,
              LEFT_BORDER_TAG,
              livingBorderCellsToReceive);
        
        //System.out.printf("%d Received: %d\n", this.processorRank, receiveStatus.length);
        
        //
        // Parse the buffer and insert the cells into a local Hash Map
        // for simulation.
        //
        
        for (int i = 0; i < receiveStatus.length; i++) {
           Cell currentCell = livingBorderCellsToReceive.get(i);
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
}*/


