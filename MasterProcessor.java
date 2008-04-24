import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import edu.rit.pj.Comm;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.IntegerBuf;
import edu.rit.util.Arrays;
import edu.rit.util.Range;

public class MasterProcessor extends Processor
{
   public MasterProcessor(
         int nProcessorRank, 
         int nNumProcessors, 
         int nNumGridRows, 
         int nNumGridCols, 
         int nNumAliveCells, 
         int nNumIterationsToRun,
         String strInputFilename)
   {
      try
      {
         
      this.m_nProcessorRank = nProcessorRank;
      this.m_nNumProcessors = nNumProcessors;
      this.m_nNumIterationsToRun = nNumIterationsToRun;
      m_CommWorld = Comm.world();
      this.LogMessage("------------------------------");

      FileReader oInputFile = new FileReader(strInputFilename);
      BufferedReader oReadBuffer = new BufferedReader(oInputFile);

      this.m_nNumGridRows = nNumGridRows;
      this.m_nNumGridCols = nNumGridCols;
      this.m_nNumAliveCells = nNumAliveCells;
      
      m_aGlobalGameBoard = new Cell[m_nNumGridRows][m_nNumGridCols];
      // Arrays.allocate<Cell,Cell>(m_aGlobalGameBoard, new Range(0, m_nNumGridRows), m_nNumGridCols, new Cell());
      
      //
      // Initialize all Cell Objects.
      //
      
      for (int i = 0; i < this.m_nNumGridRows; i++)
      {
         for (int j = 0; j < this.m_nNumGridCols; j++)
         {
            m_aGlobalGameBoard[i][j] = new Cell(Cell.LifeState.Dead, i, j);
         }
      }
      
      //
      // During initialization, set predefined Cells as Alive.
      //
      
      int nAliveCellRow = 0;
      int nAliveCellCol = 0;
      
      String strLine = "";;
      String[] aLineTokens;
      
      for (int i = 0; i < this.m_nNumAliveCells; i++)
      {
         strLine = oReadBuffer.readLine();
         aLineTokens = strLine.split(" ");
         
         nAliveCellRow = Integer.parseInt(aLineTokens[0]);
         nAliveCellCol = Integer.parseInt(aLineTokens[1]);
         
         //
         // Skip invalid Row / Column combinations.
         //
         
         if ((nAliveCellRow < 0) || (nAliveCellRow >= this.m_nNumGridRows) || 
             (nAliveCellCol < 0) || (nAliveCellCol >= this.m_nNumGridCols))
         {
            continue;
         }
         
         m_aGlobalGameBoard[nAliveCellRow][nAliveCellCol].m_CurrentState = Cell.LifeState.Alive;
      }
      
      oReadBuffer.close();
      }
      catch (IOException e)
      {
         this.LogMessage("Error Reading File: " + strInputFilename);
      }
   }
   
   public void DistributeDataToWorkers()
   {
      //
      // Partition the Global Game Board into fixed sized chunks. In PJ, 
      // objects are communicated to other processors via Object Serialization.
      //
      
      Range[] colRanges = new Range(0, this.m_nNumGridCols-1).subranges(this.m_nNumProcessors - 1);
      ObjectBuf<Cell>[] aMatrixColSlices = ObjectBuf.colSliceBuffers(m_aGlobalGameBoard, colRanges);
      
      for (int nDestProcessorRank = 1; nDestProcessorRank < this.m_nNumProcessors; nDestProcessorRank++)
      {
         try
         {
            m_CommWorld.send(nDestProcessorRank, aMatrixColSlices[nDestProcessorRank-1]);
         }
         catch (IOException e)
         {
            this.LogMessage("Master could not send to Processor: " + nDestProcessorRank);
         }
      }
   }
   
   //
   // Main Processing Loop
   //
   
   public void DoWork()
   {
      this.LogMessage("Master Processor Exiting!");
      System.exit(-1);
      
       while (true)
       {
          //
          // Give the CPU a break.
          //
          
          try
          {
             Thread.sleep(1000);
          }
          catch(InterruptedException e)
          {
             System.exit(-1);
          }
       }
   }
   
   public Cell[][] GetGlobalGameBoard()
   {
      return m_aGlobalGameBoard;
   }
   
   public boolean HasAliveCells()
   {
      return m_nNumAliveCells > 0;
   }
   
   public int GetNumGridRows()
   {
      return m_nNumGridRows;
   }
   
   public int GetNumGridCols()
   {
      return m_nNumGridCols;  
   }
}
