import java.io.IOException;
import edu.rit.pj.Comm;
import edu.rit.pj.CommStatus;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.buf.BooleanMatrixBuf;
import edu.rit.mp.BooleanBuf;

public class WorkerProcessor extends Processor {

	public WorkerProcessor(
			int nProcessorRank, 
			int nNumProcessors, 
			int nNumGridRows, 
			int nNumGridCols)
	{
		super(nProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols);
   }
   
   public void ReceiveDataFromMaster()
   {
      //
      // Wait for data from the Master before proceeding.
      //

      BooleanBuf aMatrixSliceToReceive = BooleanBuf.buffer(m_aGlobalGameBoard);
      CommStatus receiveStatus = null;
      
      try
      {
         receiveStatus = m_CommWorld.receive(0, aMatrixSliceToReceive);
         this.LogMessage("RECEIVED at: " + this.m_nProcessorRank);
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
      System.out.println("CREATED ZONE MANAGER AT: " + this.m_nProcessorRank + 
            "receive buffer " + nReceiveBufferLength + " ROWS NEEDED: " + nNumRowsNeeded + " AND COLS: " +
            nNumColsNeeded);
   }

}
