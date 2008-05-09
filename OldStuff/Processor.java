import edu.rit.mp.ObjectBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.CommStatus;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Date;
import java.text.SimpleDateFormat;

public abstract class Processor {
	protected static Comm m_CommWorld;
	protected int m_nNumGridRows;
	protected int m_nNumGridCols;
	protected int m_nNumAliveCells;
	protected int m_nNumIterationsToRun;
	protected Cell[][] m_aGlobalGameBoard;
   
	protected int m_nProcessorRank;
	protected int m_nNumProcessors;

	private FileWriter m_LogFileWriter;
	private File m_fLog;
	private File m_fResult;
   
	protected ZoneManager m_ZoneManager;
	protected int m_nLeftNeighborRank, m_nRightNeighborRank;

   
	/**
	 * Default constructor.  Creates files for output, instantiates class
	 * objects.
	 */
	public Processor(
	         int nProcessorRank, 
	         int nNumProcessors, 
	         int nNumGridRows, 
	         int nNumGridCols)
	{
		this.m_nProcessorRank = nProcessorRank;
	    this.m_nNumProcessors = nNumProcessors;
	    this.m_nNumGridRows = nNumGridRows;
	    this.m_nNumGridCols = nNumGridCols;
	    m_CommWorld = Comm.world();
	      
	    m_aGlobalGameBoard = new Cell[m_nNumGridRows][m_nNumGridCols];
	    for (int i = 0; i < m_nNumGridRows; i++) {
	    	for (int j = 0; j < m_nNumGridCols; j++) {
	    		m_aGlobalGameBoard[i][j] = new Cell(Cell.LifeState.Dead, i, j);
	    	}
	    }
	    
	    String strFilename = "Log/Processor_" + this.m_nProcessorRank + ".txt";
	    File outputFile = new File(strFilename);
	    outputFile.delete();
	}
   
	/**
	 * Workhorse of the Processor.  Performs the Game of Life's next state 
	 * calculations.
	 */
	public void DoWork(int iterations) {
		int nIterationCounter = 0;

		while (nIterationCounter < iterations) {
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

	public void SendBorders() {
		if (m_nLeftNeighborRank != -1) {
			ObjectBuf<Cell> aLeftZoneBorderToSend = this.m_ZoneManager.GetBorder(ZoneManager.BorderTag.Left);
	         
			try {
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
	      
		if (m_nRightNeighborRank != -1) {
			ObjectBuf<Cell> aRightZoneBorderToSend = this.m_ZoneManager.GetBorder(ZoneManager.BorderTag.Right);
	         
			try {
				m_CommWorld.send(
						m_nRightNeighborRank, 
						ZoneManager.RIGHT_BORDER_TAG, 
						aRightZoneBorderToSend);
				this.LogMessage("Sent to Right Processor: " + m_nRightNeighborRank);
			}
			catch (IOException e) {
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
	   
	public void ReceiveBorders() {
		if (m_nLeftNeighborRank != -1) {
			ObjectBuf<Cell> aBorderToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);
			CommStatus receiveStatus = null;
	         
			try {
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
			catch (IOException e) {
				this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Left Border to Processor: " + m_nLeftNeighborRank);
			}
		}
	      
		if (m_nRightNeighborRank != -1) {
			ObjectBuf<Cell> aBorderToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);
			CommStatus receiveStatus = null;
	         
			try {
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
			catch (IOException e) {
				this.LogMessage("Processor: " + m_nProcessorRank + " could not " + "send Right Border to Processor: " + m_nRightNeighborRank);
			}
		}
	}
	   
	//
	// After each iteration, write each Zone Manager's Local Matrix to File.
	//
	   
	public void SaveIterationData() {
		Cell[][] aFrontBoardArea = m_ZoneManager.GetFrontGameBoard();
		String strData = "\n";
	      
		for (int i = 0; i < m_ZoneManager.GetNumZoneRows(); i++) {
			for (int j = 0; j < m_ZoneManager.GetNumZoneCols(); j++) {
				if (aFrontBoardArea[i][j].m_CurrentState == Cell.LifeState.Alive) {
					strData += "X ";
				}
				else {
					strData += "_ ";
				}
			}
	         
			strData += "\n";
		}
	      
		this.LogMessage(strData);
	}
	   
	/**
	 * Provides log file functionality for debugging communications on multiple
	 * processors.
	 */
   protected void LogMessage(String strMessage) {
	  
	   try {
		   String strFilename = "Log/Processor_" + this.m_nProcessorRank + ".txt";
		   m_LogFileWriter = new FileWriter(strFilename, true);
	   }
	   catch (IOException e) {
		   System.out.println(e.toString());
		   return;
	   }
      
	   SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss a zzz");
	   Date dCurrentTime = new Date();
	   String dateString = formatter.format(dCurrentTime);
      
	   try {
		   m_LogFileWriter.write(dateString + ": " + strMessage + "\n");
		   m_LogFileWriter.close();
	   }
	   catch (IOException e) {
		   return;
	   }      
   	}
}
