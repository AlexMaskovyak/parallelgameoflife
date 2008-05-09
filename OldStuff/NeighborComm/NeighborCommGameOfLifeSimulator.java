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
 * SequentialGameOfLifeSimulator is responsible for storing game logic, storing
 * game-state, and performing the simulation.
 * @author Alex Maskovyak
 *
 */
public class NeighborCommGameOfLifeSimulator implements GameOfLifeSimulator {

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
   private ConwayCellNeighborhood neighborhood;
   
   private int numLivingCellsInFile;
   private int myProcessorRank;
   private int leftProcessorRank;
   private int rightProcessorRank;
   private int numProcessors;
   private int largestSliceToReceive;
   private int leftBorderYBound;
   private int rightBorderYBound;
   private int currentIteration;
   
   public static final int LEFT_BORDER_TAG = 0;
   public static final int RIGHT_BORDER_TAG = 1;
   
   /**
    * Default constructor.
    * @param rules Rules to determine a cell's next state.
    * @param neighborhood Determines cell connectivity.
    */
   public NeighborCommGameOfLifeSimulator(
         CellLifeRules rules, 
         ConwayCellNeighborhood neighborhood,
         int numLivingCellsInFile,
         int myProcessorRank,
         int numProcessors) {
      this(new ArrayList<Cell>(), rules, neighborhood, numLivingCellsInFile, myProcessorRank, numProcessors);
   }
   
   /**
    * Constructor.
    * @param liveCells List of live cells to manage.
    * @param rules Rules to determine a cell's next state.
    * @param neighborhood Determines cell connectivity.
    */
   public NeighborCommGameOfLifeSimulator(
         List<Cell> liveCells, 
         CellLifeRules rules, 
         ConwayCellNeighborhood neighborhood,
         int numLivingCellsInFile,
         int myProcessorRank,
         int numProcessors) 
   {
      // set our internal values
      this.livingCells = new HashMap<Cell, Cell>();
      this.gestatingCells = new HashMap<Cell, Cell>();

      this.cellsToRemove = new ArrayList<Cell>();
      
      this.rules = rules;
      this.neighborhood = neighborhood;
      
      // add live cells to the living cell storage area
      this.addLivingCells(liveCells);
      
      this.numLivingCellsInFile = numLivingCellsInFile;
      this.myProcessorRank = myProcessorRank;
      this.numProcessors = numProcessors;
      this.largestSliceToReceive = (this.neighborhood.xMaxBounds * this.neighborhood.yMaxBounds);
      this.leftBorderYBound = 100000000;
      this.rightBorderYBound = 0;
      this.currentIteration = 0;
      
      if (this.myProcessorRank - 1 == -1)
      {
         // No Wrapping for now, so make leftmost processor's left neighbor invalid.
         this.leftProcessorRank = -1;
      }
      else
      {
         this.leftProcessorRank = (this.myProcessorRank - 1) % this.numProcessors;
      }
      
      //
      // Compute Right Neighbor
      //
      
      if (this.myProcessorRank + 1 == this.numProcessors)
      {
         // No Wrapping for now, so make rightmost processor's right neighbor invalid.
         this.rightProcessorRank = -1;
      }
      else
      {
         this.rightProcessorRank = (this.myProcessorRank + 1) % this.numProcessors;
      }
   }
   
   /**
    * Master Processor method to send Living Cell Slices
    * to each available Worker Processor.
    */
   
   public void distributeDataToWorkers(List<Cell> livingCells) {
      Range[] colRanges = new Range(0, livingCells.size()-1).subranges(this.numProcessors);
      Cell[] livingCellHolder = new Cell[livingCells.size()];
      
      for (int i = 0; i < livingCells.size(); i++)
      {
         livingCellHolder[i] = livingCells.get(i);
      }
      
      ObjectBuf<Cell>[] livingCellSlices = ObjectBuf.sliceBuffers(livingCellHolder, colRanges);
      CommRequest request = new CommRequest();
      
      for (int nDestProcessorRank = 1; nDestProcessorRank < this.numProcessors; nDestProcessorRank++) {
         try {
            Comm.world().send(nDestProcessorRank, livingCellSlices[nDestProcessorRank]);
            
            //
            // Add First Slice to Master's Simulation Hash Map so that it
            // can also perform computations.
            //
            
            int firstSliceSize = livingCells.size() / this.numProcessors;
            for (int i = 0; i < firstSliceSize; i++)
            {
               Cell currentCell = livingCells.get(i);
               if (currentCell.y < this.leftBorderYBound)
               {
                  this.leftBorderYBound = currentCell.y;
               }
               else if (currentCell.y > this.rightBorderYBound)
               {
                  this.rightBorderYBound = currentCell.y;
               }
               
               this.addLivingCell(currentCell);
            }
         }
         catch (IOException e) {
            System.out.println("Could not send to Processor: " + nDestProcessorRank);
         }
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
      CommStatus receiveStatus = null;
      
      try
      {
         receiveStatus = Comm.world().receive(0, livingCellsToReceive);
         System.out.println("Processor: " + this.myProcessorRank + " received data of size " + receiveStatus.length);
         
         //
         // Parse the buffer and insert the cells into a local Hash Map
         // for simulation. Also, track which the min and max columns that this
         // Worker receives as this information will be needed for border exchanging.
         //
         
         List<Cell> livingCells = new ArrayList<Cell>();
         for (int i = 0; i < receiveStatus.length; i++)
         {
            Cell currentCell = livingCellsToReceive.get(i);
            if (currentCell.y < this.leftBorderYBound)
            {
               this.leftBorderYBound = currentCell.y;
            }
            else if (currentCell.y > this.rightBorderYBound)
            {
               this.rightBorderYBound = currentCell.y;
            }
            
            livingCells.add(currentCell);
         }
         
         this.addLivingCells(livingCells);
      }
      catch (IOException e)
      {
         System.out.println("Processor: " + this.myProcessorRank + " could not receive data");
      }
   }
   
   /**
    * At each iteration, the Worker Processor will send its 'Border' Cells
    * to its designated neighbors.
    */
   public void sendLeftBorder()
   {
      //
      // Create left and right borders to send. Since the leftmost and
      // or rightmost border values may be the same as in other processors,
      // create a (+/- 1) buffer for determining them.
      //
      
      int nLeftBorderCounter = 0;
      Iterator<Cell> livingCellIterator = livingCells.keySet().iterator();
      
      while(livingCellIterator.hasNext()) 
      {
         Cell currentCell = livingCellIterator.next();
         
         if ((currentCell.y == this.leftBorderYBound) || 
             (currentCell.y == this.leftBorderYBound + 1))
         {
            nLeftBorderCounter++;
         }
      }
      
      Cell[] livingLeftBorderCellHolder = new Cell[nLeftBorderCounter];
      nLeftBorderCounter = 0;
      
      Iterator<Cell> livingCellIterator2 = livingCells.keySet().iterator();
      while(livingCellIterator2.hasNext()) 
      {
         Cell currentCell  = livingCellIterator2.next();
         
         if ((currentCell.y == this.leftBorderYBound) || 
             (currentCell.y == this.leftBorderYBound + 1))
         {
            livingLeftBorderCellHolder[nLeftBorderCounter] = currentCell;
            nLeftBorderCounter++;
         }
      }
      
      //
      // Send the Buffers to Neighboring Processors
      //
      
      if (this.leftProcessorRank != -1)
      {
         ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingLeftBorderCellHolder);
         CommRequest request = new CommRequest();
         
         try
         {
            Comm.world().send(
                  this.leftProcessorRank, 
                  LEFT_BORDER_TAG, 
                  livingBorderCellSlices,
                  request);
            
            //System.out.println("Processor " + this.myProcessorRank + " SENT TO LEFT: " + this.leftProcessorRank);
         }
         catch (IOException e)
         {
            System.out.println("Processor: " + this.myProcessorRank + " could not " + 
                  "send Left Border to Processor: " + this.leftProcessorRank);
         }
      }
   }
   
   /**
    * At each iteration, the Worker Processor will send its 'Border' Cells
    * to its designated neighbors.
    */
   public void sendRightBorder()
   {
      //
      // Create left and right borders to send. Since the leftmost and
      // or rightmost border values may be the same as in other processors,
      // create a (+/- 1) buffer for determining them.
      //
      
      int nRightBorderCounter = 0;
      Iterator<Cell> livingCellIterator = livingCells.keySet().iterator();
      
      while(livingCellIterator.hasNext()) 
      {
         Cell currentCell = livingCellIterator.next();
         
         if ((currentCell.y == this.rightBorderYBound) || 
               (currentCell.y == this.rightBorderYBound - 1))
         {
            nRightBorderCounter++;
         }
      }
      
      Cell[] livingRightBorderCellHolder = new Cell[nRightBorderCounter];
      nRightBorderCounter = 0;
      
      Iterator<Cell> livingCellIterator2 = livingCells.keySet().iterator();
      while(livingCellIterator2.hasNext()) 
      {
         Cell currentCell  = livingCellIterator2.next();
         if ((currentCell.y == this.rightBorderYBound) || 
               (currentCell.y == this.rightBorderYBound - 1))
         {
            livingRightBorderCellHolder[nRightBorderCounter] = currentCell;
            nRightBorderCounter++;
         }
      }
      
      //
      // Send the Buffers to Neighboring Processors
      //
      
      if (this.rightProcessorRank != -1)
      {
         ObjectBuf<Cell> livingBorderCellSlices = ObjectBuf.buffer(livingRightBorderCellHolder);
         CommRequest request = new CommRequest();
         
         try
         {
            Comm.world().send(
                  this.rightProcessorRank, 
                  RIGHT_BORDER_TAG, 
                  livingBorderCellSlices,
                  request);
            
            //System.out.println("Processor " + this.myProcessorRank + " SENT TO RIGHT: " + this.rightProcessorRank);
         }
         catch (IOException e)
         {
            System.out.println("Processor: " + this.myProcessorRank + " could not " + 
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
   
   public void receiveLeftBorder()
   {
      if (this.leftProcessorRank != -1)
      {
         //
         // Only allocate for the largest possible slice.
         //
         
         Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
         
         //
         // Wait for data from the Master before proceeding.
         //

         ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
         CommStatus receiveStatus = null;
         
         try
         {
            receiveStatus = Comm.world().receive(
                  this.leftProcessorRank, 
                  RIGHT_BORDER_TAG, 
                  livingBorderCellsToReceive);
            
            System.out.println("Iteration " + this.currentIteration + ": Processor: " + 
                  this.myProcessorRank + " received data of size " + receiveStatus.length + " AND LARGE SLICE IS: " + 
                  this.largestSliceToReceive);
            
            //
            // Parse the buffer and insert the cells into a local Hash Map
            // for simulation.
            //
            
            for (int i = 0; i < receiveStatus.length; i++)
            {
               Cell currentCell = livingBorderCellsToReceive.get(i);
               if (currentCell != null)
               {
                  currentCell.isBorderCell = true;
                  this.addLivingCell(currentCell);
               }
            }
         }
         catch (IOException e)
         {
            System.out.println("Processor: " + this.myProcessorRank + " could not " + 
                  "receive Left Border from Processor: " + this.leftProcessorRank);
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
   
   public void receiveRightBorder()
   {
      if (this.rightProcessorRank != -1)
      {
         //
         // Only allocate for the largest possible slice.
         //
         
         Cell[] receiveCellBuffer = new Cell[this.largestSliceToReceive];
         
         //
         // Wait for data from the Master before proceeding.
         //

         ObjectBuf<Cell> livingBorderCellsToReceive = ObjectBuf.buffer(receiveCellBuffer);
         CommStatus receiveStatus = null;
         
         try
         {
            receiveStatus = Comm.world().receive(
                  this.rightProcessorRank, 
                  LEFT_BORDER_TAG, 
                  livingBorderCellsToReceive);
            
            System.out.println("Iteration " + this.currentIteration + ": Processor: " + 
                  this.myProcessorRank + " received data of size " + receiveStatus.length + 
                  " AND LARGE SLICE IS: " + this.largestSliceToReceive);
            
            //
            // Parse the buffer and insert the cells into a local Hash Map
            // for simulation.
            //

            for (int i = 0; i < receiveStatus.length; i++)
            {
               Cell currentCell = livingBorderCellsToReceive.get(i);
               if (currentCell != null)
               {
                  currentCell.isBorderCell = true;
                  this.addLivingCell(currentCell);
               }
            }
         }
         catch (IOException e)
         {
            System.out.println("Processor: " + this.myProcessorRank + " could not " + 
                  "receive Right Border from Processor: " + this.rightProcessorRank);
         }
      }
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
      
      // for the living cells, determine whether they remain alive,
      // border cells will be automatically removed for the next iteration.
      for (Cell livingCell : livingCellSet) {
         if ((!rules.lives(livingCell.neighborCount)) || (livingCell.isBorderCell)) {
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
      
      currentIteration++;
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

         //
         // Parse the buffer and insert the cells into a local Hash Map
         // for simulation. Also, track which the min and max columns that this
         // Worker receives as this information will be needed for border exchanging.
         //
         
         if (c.y < this.leftBorderYBound)
         {
            this.leftBorderYBound = c.y;
         }
         else if (c.y > rightBorderYBound)
         {
            this.rightBorderYBound = c.y;
         }

         this.livingCells.put(c, c);
      }
      
//      System.out.println("Processor " + this.myProcessorRank +
//            " has Left Border Bound: " + this.leftBorderYBound + " and " +
//            " has Right Border Bound: " + this.rightBorderYBound);
   }
   
   /**
    * Retrieves the total number of Living Cells that initially existed
    * in the full input file
    */
   public int getNumLivingCellsInFile()
   {
      return this.numLivingCellsInFile;
   }

   /**
    * Retrieves this Processor's Rank relative to the PJ Comm World.
    */
   public int getProcessorRank()
   {
      return this.myProcessorRank;
   }
   
   /**
    * Retrieves the number of processors?
    */
   public int getNumProcessors()
   {
      return this.numProcessors;
   }
}
