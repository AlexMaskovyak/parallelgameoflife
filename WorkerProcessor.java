import java.io.IOException;
import edu.rit.pj.Comm;
import edu.rit.pj.CommStatus;
import edu.rit.mp.ObjectBuf;

public class WorkerProcessor extends Processor
{
   private ZoneManager m_ZoneManager;
   private int m_nLeftNeighborRank, m_nRightNeighborRank;
   
   public WorkerProcessor(
         int nProcessorRank, 
         int nNumProcessors, 
         int nNumGridRows, 
         int nNumGridCols,
         int nNumIterationsToRun)
   {
      this.m_nProcessorRank = nProcessorRank;
      this.m_nNumProcessors = nNumProcessors;
      this.m_nNumGridRows = nNumGridRows;
      this.m_nNumGridCols = nNumGridCols;
      this.m_nNumIterationsToRun = nNumIterationsToRun;
      m_CommWorld = Comm.world();
      
      m_aGlobalGameBoard = new Cell[m_nNumGridRows][m_nNumGridCols];
      for (int i = 0; i < m_nNumGridRows; i++)
      {
         for (int j = 0; j < m_nNumGridCols; j++)
         {
            m_aGlobalGameBoard[i][j] = new Cell(Cell.LifeState.Dead, i, j);
         }
      }

      this.LogMessage("------------------------------");
      
      //
      // Compute Left Neighbor
      //
      
      if (m_nProcessorRank - 1 == 0)
      {
         //m_nLeftNeighborRank = nNumProcessors;
         
         // No Wrapping for now, so make leftmost processor's left neighbor invalid.
         m_nLeftNeighborRank = -1;
      }
      else
      {
         m_nLeftNeighborRank = (m_nProcessorRank - 1) % nNumProcessors;
      }
      
      //
      // Compute Right Neighbor
      //
      
      if (m_nProcessorRank + 1 == nNumProcessors)
      {
         //m_nRightNeighborRank = nNumProcessors;
         
         // No Wrapping for now, so make rightmost processor's right neighbor invalid.
         m_nRightNeighborRank = -1;
      }
      else
      {
         m_nRightNeighborRank = (m_nProcessorRank + 1) % nNumProcessors;
      }
   }
   
   public void ReceiveDataFromMaster()
   {
      //
      // Wait for data from the Master before proceeding.
      //

      ObjectBuf<Cell> aMatrixSliceToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);
      CommStatus receiveStatus = null;
      
      try
      {
         receiveStatus = m_CommWorld.receive(0, aMatrixSliceToReceive);
      }
      catch (IOException e)
      {
         this.LogMessage("Error Receiving");
      }

      if (receiveStatus == null)
      {
         this.LogMessage("receiveStatus is null");
         System.exit(-1);   
      }
      
      //
      // Set up the Zone Manager for the Worker Processor by calculating the
      // number of required rows and columns needed as per the received data.
      // 
      
      int nReceiveBufferLength = receiveStatus.length;
      int nNumRowsNeeded = m_nNumGridRows;
      int nNumColsNeeded = nReceiveBufferLength / m_nNumGridCols;
      
      m_ZoneManager = new ZoneManager(nNumRowsNeeded, nNumColsNeeded, aMatrixSliceToReceive);
   }
   
   //
   // Main Processing Loop
   //
   
   public void DoWork()
   {  
      int nIterationCounter = 0;
      while (nIterationCounter < m_nNumIterationsToRun)
      {
         this.SaveIterationData();
         
         //
         // Send this Processor's Border Information to its corresponding neighbors.
         //
         
         this.SendBorders();
         this.ReceiveBorders();
         
         //
         // Once bordering Cells are known, perform the Game of Life computation
         // on this Processor's Local Zone Matrix.
         //

         m_ZoneManager.PerformSimulation();
         m_ZoneManager.ClearNeighborBorderData();
         
         nIterationCounter++;
      }
   }
   
   //
   // At each iteration, the Worker Processor will send its 'Border' Cells
   // to its designated neighbors.
   //
   
   public void SendBorders()
   {
      if (m_nLeftNeighborRank != -1)
      {
         ObjectBuf<Cell> aLeftZoneBorderToSend = this.m_ZoneManager.GetBorder(ZoneManager.BorderTag.Left);
         
         try
         {
            m_CommWorld.send(
                  m_nLeftNeighborRank, 
                  ZoneManager.LEFT_BORDER_TAG, 
                  aLeftZoneBorderToSend);
            this.LogMessage("Sent to Left Processor: " + m_nLeftNeighborRank);
         }
         catch (IOException e)
         {
            this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Left Border to Processor: " + m_nLeftNeighborRank);
         }
      }
      
      if (m_nRightNeighborRank != -1)
      {
         ObjectBuf<Cell> aRightZoneBorderToSend = this.m_ZoneManager.GetBorder(ZoneManager.BorderTag.Right);
         
         try
         {
            m_CommWorld.send(
                  m_nRightNeighborRank, 
                  ZoneManager.RIGHT_BORDER_TAG, 
                  aRightZoneBorderToSend);
            this.LogMessage("Sent to Right Processor: " + m_nRightNeighborRank);
         }
         catch (IOException e)
         {
            this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Right Border to Processor: " + m_nRightNeighborRank);
         }
      }
   }
   
   //
   // At each iteration, the Worker Processor will receive its Border Cells
   // from its designated neighbors. 
   // 
   // Note: When receiving, Worker Processors will receive Right Borders from
   // their Left Neighbors and Left Borders from their Right Neighbors.
   //
   
   public void ReceiveBorders()
   {
      if (m_nLeftNeighborRank != -1)
      {
         ObjectBuf<Cell> aBorderToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);
         CommStatus receiveStatus = null;
         
         try
         {
            receiveStatus = m_CommWorld.receive(
                  m_nLeftNeighborRank, 
                  ZoneManager.RIGHT_BORDER_TAG, 
                  aBorderToReceive);
            this.LogMessage("Received from Left Processor: " + m_nLeftNeighborRank + " Buffer Length: " + receiveStatus.length);
            
            //
            // After receiving the Neighbor's Right Border, pass the info to Zone Manager,
            // as it will update its Local Matrix's Left Border.
            //
            
            m_ZoneManager.UpdateBorder(aBorderToReceive, receiveStatus.length, ZoneManager.BorderTag.Left);
         }
         catch (IOException e)
         {
            this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Left Border to Processor: " + m_nLeftNeighborRank);
         }
      }
      
      if (m_nRightNeighborRank != -1)
      {
         ObjectBuf<Cell> aBorderToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);
         CommStatus receiveStatus = null;
         
         try
         {
            receiveStatus = m_CommWorld.receive(
                  m_nRightNeighborRank, 
                  ZoneManager.LEFT_BORDER_TAG, 
                  aBorderToReceive);
            this.LogMessage("Received from Right Processor: " + m_nRightNeighborRank + " Buffer Length: " + receiveStatus.length);
            
            //
            // After receiving the Neighbor's Left Border, pass the info to Zone Manager,
            // as it will update its Local Matrix's Right Border.
            //
            
            m_ZoneManager.UpdateBorder(aBorderToReceive, receiveStatus.length, ZoneManager.BorderTag.Right);
         }
         catch (IOException e)
         {
            this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Right Border to Processor: " + m_nRightNeighborRank);
         }
      }
   }
   
   //
   // After each iteration, write each Zone Manager's Local Matrix to File.
   //
   
   public void SaveIterationData()
   {
      Cell[][] aFrontBoardArea = m_ZoneManager.GetFrontGameBoard();
      String strData = "\n";
      
      for (int i = 0; i < m_ZoneManager.GetNumZoneRows(); i++)
      {
         for (int j = 0; j < m_ZoneManager.GetNumZoneCols(); j++)
         {
            if (aFrontBoardArea[i][j].m_CurrentState == Cell.LifeState.Alive)
            {
               strData += "X ";
            }
            else
            {
               strData += "_ ";
            }
         }
         
         strData += "\n";
      }
      
      this.LogMessage(strData);
   }
}
