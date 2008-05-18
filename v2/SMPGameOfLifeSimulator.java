import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.rit.pj.ParallelTeam;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelSection;
import edu.rit.pj.IntegerForLoop;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MasterCommGameOfLifeSimulator is responsible for storing game logic, storing
 * game-state, and performing the simulation.
 * @author Sean Janis 
 * @author Alex Maskovyak
 * @author Steve Baylor
 *
 */
public class SMPGameOfLifeSimulator extends SequentialGameOfLifeSimulator {
   
   /**
    * Constructor.
    * @param liveCells List of live cells to manage.
    * @param rules Rules to determine a cell's next state.
    * @param neighborhood Determines cell connectivity.
    */
   public SMPGameOfLifeSimulator(
         CellLifeRules rules, 
         CellNeighborhood neighborhood,
         File cellsFile)   
   {
      // call superconstructor
      super(new ArrayList<Cell>(), rules, neighborhood);
      List<Cell> livingCells = GameOfLifeFileIO.getLiveCells(cellsFile);
      
      for (Cell c : livingCells)
      {
         this.addLivingCell(c);
      }
   }

   
   /* (non-Javadoc)
    * @see GameOfLifeSimulator#performSimulation()
    */
   public void performSimulation() throws Exception {
      
      //
      // Perform necessary cleanup before Simulation
      //

      this.cleanCellCounts();
 
      //
      // Convert Living Cell Hash Map to Chunkable List
      //
      
      Set<Cell> livingCellsSet = livingCells.keySet();
      final List<Cell> livingCellsList = new ArrayList<Cell>();
  
      for (Cell currentCell : livingCellsSet) 
      {
         livingCellsList.add(currentCell);
      }
      
      new ParallelTeam().execute(new ParallelRegion()
      {
         public void run() throws Exception
         {
            final int myTeamID = getThreadIndex() + 1;
            
            //
            // Each Parallel Team Thread has an Alive Cell chunk to process.
            //
            
            execute(0, livingCellsList.size(), new IntegerForLoop()
            {
               HashMap<Cell, Cell> localLivingCells = new HashMap<Cell, Cell>();
               
               public void run(int first, int last) throws Exception
               {
                  //System.out.println("Thread " + myTeamID + ": Living Cells Range " + first + "-" + last);

                  for (int i = first; i <= last; i++)
                  {
                     if (i == livingCellsList.size())
                     {
                        continue;
                     }
                     
                     Cell temp = livingCellsList.get(i);
                     if (temp == null)
                     {
                        continue;   
                     }
                     
                     localLivingCells.put(temp, temp);
                     //System.out.println("Thread " + myTeamID + ": " + temp.toString());
                  }
                  
                  //
                  // Uses this Parallel Thread's Living Cell chunk to update
                  // the shared Living Cells HashMap
                  //
                  
                  updateCellCounts(localLivingCells);
               }
            });
         }
      });
      
      //
      // After all data chunks have been analyzed and stored back in the
      // shared Living Cell HashMap, update the Living Cells for the next iteration.
      //
      
      this.updateLiveCellList();
   }
}


