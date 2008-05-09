import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import edu.rit.pj.Comm;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.IntegerBuf;
import edu.rit.util.Arrays;
import edu.rit.util.Range;

public class MasterProcessor extends Processor {
   
	public MasterProcessor (
			int nProcessorRank, 
			int nNumProcessors, 
			int nNumGridRows, 
			int nNumGridCols, 
			int nNumAliveCells, 
			int nNumIterationsToRun,
			String strInputFilename)
	{
		
		super(nProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols);
		
		try {
         
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
      
			for (int i = 0; i < this.m_nNumGridRows; i++) {
				for (int j = 0; j < this.m_nNumGridCols; j++) {
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
      
			for (int i = 0; i < this.m_nNumAliveCells; i++) {
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
		catch (IOException e) {
			this.LogMessage("Error Reading File: " + strInputFilename);
        }
	}
   
	public void DistributeDataToWorkers() {
		//
		// Partition the Global Game Board into fixed sized chunks. In PJ, 
		// objects are communicated to other processors via Object Serialization.
		//
      
		Range[] colRanges = new Range(0, this.m_nNumGridCols - 1).subranges(this.m_nNumProcessors);
		ObjectBuf<Cell>[] aMatrixColSlices = ObjectBuf.colSliceBuffers(m_aGlobalGameBoard, colRanges);
      
		for (int nDestProcessorRank = 1; nDestProcessorRank < this.m_nNumProcessors; nDestProcessorRank++) {
			try {
				m_CommWorld.send(nDestProcessorRank, aMatrixColSlices[nDestProcessorRank]);
			}
			catch (IOException e) {
				this.LogMessage("Master could not send to Processor: " + nDestProcessorRank);
			}
		}
		
		//
		// Set up the Zone Manager for the Worker Processor by calculating the
		// number of required rows and columns needed as per the received data.
		// 
		
		int nNumRowsNeeded = m_nNumGridRows;
	    int nNumColsNeeded = aMatrixColSlices[0].length() / nNumRowsNeeded;
	    
	    super.LogMessage("My grid rows: " + nNumRowsNeeded);
	    super.LogMessage("My grid cols: " + nNumColsNeeded);
	    //aMatrixColSlices[0].
		//int nNumColsNeeded = aMatrixColSlices[0].length();
	    //int nReceiveBufferLength = receiveStatus.length;
		//int nNumRowsNeeded = m_nNumGridRows;
		//int nNumColsNeeded = nReceiveBufferLength / m_nNumGridCols;
	      
		m_ZoneManager = new ZoneManager(nNumRowsNeeded, nNumColsNeeded, aMatrixColSlices[0]);
	}
   
	public void DistributeDataToAll() {
		//
		// Partition the Global Game Board into fixed sized chunks.  In PJ,
		// objects are communicated to other processors via Object Serialization.
		//
		
		Range[] colRanges = new Range(0, this.m_nNumGridCols - 1).subranges(this.m_nNumProcessors);
		ObjectBuf<Cell>[] aMatrixColSlices = ObjectBuf.colSliceBuffers(m_aGlobalGameBoard, colRanges);
      
		ObjectBuf<Cell> aMatrixSliceToReceive = ObjectBuf.buffer(m_aGlobalGameBoard);

		try {
			m_CommWorld.scatter(0, aMatrixColSlices, aMatrixSliceToReceive);
		}
		catch (IOException e) {
			this.LogMessage("Master could not send to Processors");
		}
		
		
	}
	
	//
	// Main Processing Loop
	//
   
	public void DoWork(int iterations) {
		super.DoWork(iterations);
		
		//this.LogMessage("Master Processor Exiting!");
		//return;      
	}
   
	public Cell[][] GetGlobalGameBoard() {
		return m_aGlobalGameBoard;
	}
   
	public boolean HasAliveCells() {
		return m_nNumAliveCells > 0; 
	}
   
   	public int GetNumGridRows() {
	   return m_nNumGridRows;
   	}
   
   	public int GetNumGridCols()	{
	   return m_nNumGridCols;  
   	}
}
