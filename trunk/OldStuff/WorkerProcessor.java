import java.io.IOException;
import edu.rit.pj.Comm;
import edu.rit.pj.CommStatus;
import edu.rit.mp.ObjectBuf;

public class WorkerProcessor extends Processor {
	//private ZoneManager m_ZoneManager;
	//private int m_nLeftNeighborRank, m_nRightNeighborRank;
   
	public WorkerProcessor(
			int nProcessorRank, 
			int nNumProcessors, 
			int nNumGridRows, 
			int nNumGridCols)
	{
		super(nProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols);

		this.LogMessage("------------------------------");
	      
	    //
	    // Compute Left Neighbor
	    //
	      
	    if (m_nProcessorRank == 0) {        
	    	// No Wrapping for now, so make leftmost processor's left neighbor invalid.
	    	m_nLeftNeighborRank = -1;
	    }
	    else {
	    	m_nLeftNeighborRank = (m_nProcessorRank - 1) % nNumProcessors;
	    }
	      
	    //
	    // Compute Right Neighbor
	    //
	      
	    if (m_nProcessorRank + 1 == nNumProcessors) {
	    	//m_nRightNeighborRank = nNumProcessors;
	         
	    	// No Wrapping for now, so make rightmost processor's right neighbor invalid.
	    	m_nRightNeighborRank = -1;
	    }
	    else {
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
      
      super.LogMessage("My rows needed: " + nNumRowsNeeded);
      super.LogMessage("My cols needed: " + nNumColsNeeded);
      
      m_ZoneManager = new ZoneManager(nNumRowsNeeded, nNumColsNeeded, aMatrixSliceToReceive);
   }
   
	/* public void DistributeDataToAll() {
		//
		// Partition the Global Game Board into fixed sized chunks.  In PJ,
		// objects are communicated to other processors via Object Serialization.
		//
		
		ObjectBuf<Cell> aMatrixSliceToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);

		try {
			m_CommWorld.scatter(0, null, aMatrixSliceToReceive);
		}
		catch (IOException e) {
			this.LogMessage("Worker could not receive info from Master");
		}
		
	    //
	    // Set up the Zone Manager for the Worker Processor by calculating the
	    // number of required rows and columns needed as per the received data.
	    // 
	      
	    //int nReceiveBufferLength = receiveStatus.length;
	    int nNumRowsNeeded = m_nNumGridRows;
	    int nNumColsNeeded = m_nNumGridCols / m_CommWorld.size();
	      
	    m_ZoneManager = new ZoneManager(nNumRowsNeeded, nNumColsNeeded, aMatrixSliceToReceive);
	}*/
   
   
   //
   // Main Processing Loop
   //
   
   
   //
   // At each iteration, the Worker Processor will send its 'Border' Cells
   // to its designated neighbors.
   //
   

}
