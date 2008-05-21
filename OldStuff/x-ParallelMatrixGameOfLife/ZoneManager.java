import edu.rit.mp.buf.BooleanMatrixBuf;
import edu.rit.mp.BooleanBuf;
import edu.rit.pj.Comm;
import edu.rit.util.Range;

public class ZoneManager
{
   private boolean[][] m_aFrontBoardArea;
   private boolean[][] m_aBackBoardArea;
   private boolean[][] m_aTempBoardArea;
   private int m_nNumRows, m_nNumCols;
   
   public static final int LEFT_BORDER_TAG = 0;
   public static final int RIGHT_BORDER_TAG = 1;
   
   public static enum BorderTag
   {
      Left,
      Right
   };
   
   public ZoneManager(int nNumRows, int nNumCols, BooleanBuf aReceivedSlice)
   {
      this.m_nNumRows = nNumRows;
      this.m_nNumCols = nNumCols;
      
      //
      // Since this class will also need to manage bordering cells around
      // an allocated Zone, increase the matrix size to have a one cell border.
      //
      
      this.m_nNumRows += 2;
      this.m_nNumCols += 2;
      
      m_aFrontBoardArea = new boolean[this.m_nNumRows][this.m_nNumCols];
      m_aBackBoardArea = new boolean[this.m_nNumRows][this.m_nNumCols];
      
      for (int i = 0; i < this.m_nNumRows; i++)
      {
         for (int j = 0; j < this.m_nNumCols; j++)
         {
            m_aFrontBoardArea[i][j] = false;
            m_aBackBoardArea[i][j] = false;
         }
      }
      
      int nSliceItemCounter = 0;
      for (int i = 1; i < this.m_nNumRows - 1; i++)
      {
         for (int j = 1; j < this.m_nNumCols - 1; j++)
         {
            m_aFrontBoardArea[i][j] = aReceivedSlice.get(nSliceItemCounter);
            nSliceItemCounter++;
         }
      }
   }
   
   //
   // Returns a ready-to-send Parallel Java Object Buffer for this Zone's 
   // Left or Right Border.
   //
   
   public BooleanBuf GetBorder(BorderTag eBorderTag)
   {
      Range[] rowRanges = new Range(1, m_nNumRows - 2).subranges(1);
      Range[] colRanges = new Range(0, m_nNumCols - 1).subranges(m_nNumCols);
      
      BooleanBuf[] aMatrixColSlices = BooleanMatrixBuf.patchBuffers(m_aFrontBoardArea, rowRanges, colRanges);
      
      if (eBorderTag == BorderTag.Left)
      {
         return aMatrixColSlices[1];
      }
      else
      {
         return aMatrixColSlices[m_nNumCols - 2];
      }
   }

   //
   // Invoking after receiving a Neighbor's Border(s). Used to update
   // the Zone Manager's Local Matrix for future computations.
   //
   
   public void UpdateBorder(
         BooleanBuf aReceivedSlice, 
         int nReceivedSliceSize, 
         BorderTag eBorderTag)
   {
      //
      // Each slice is a column, therefore, update each row corresponding to the
      // specified column border tag.
      //
      
      int nColumnToUpdate = -1;
      if (eBorderTag == BorderTag.Left)
      {
         nColumnToUpdate = 0;
      }
      else
      {
         nColumnToUpdate = m_nNumCols - 1;
      }
      
      int nReceivedSliceCounter = 0;
      for (int i = 1; i < this.m_nNumRows - 1; i++)
      {
         m_aFrontBoardArea[i][nColumnToUpdate] = aReceivedSlice.get(nReceivedSliceCounter);
         nReceivedSliceCounter++;
      }
   }
   
   public void ClearNeighborBorderData()
   {
      int nLeftBorderColumn = 0;
      int nRightBorderColumn = m_nNumCols - 1;
         
      for (int nRow = 0; nRow < this.m_nNumRows; nRow++)
      {
         m_aFrontBoardArea[nRow][nLeftBorderColumn] = false;
         m_aFrontBoardArea[nRow][nRightBorderColumn] = false;
      }
   }
   
   //
   // For each cell in the 'Front Buffer', compute its next value in the
   // 'Back Buffer' by using the Game of Life's Live / Die rules, 
   // then swap the matrices.
   //
   // Rules:
   //
   // 1. Any live cell with fewer than two live neighbors dies, as if by loneliness.
   // 2. Any live cell with more than three live neighbors dies, as if by overcrowding.
   // 3. Any live cell with two or three live neighbors lives, unchanged, to the next generation.
   // 4. Any dead cell with exactly three live neighbors comes to life.
   //
   
   public void PerformSimulation()
   {
      int nTempNeighborCount = 0;
      int nX = 0;
      int nY = 0;
      
      //
      // Only need to iterate over Zone-specific cells, therefore start at 1,1 
      // and end at r-1, c-1 as the 'padding' doesn't need to be considered.
      //
      
      for (int nRow = 1; nRow < this.m_nNumRows - 1; nRow++)
      {
         for (int nCol = 1; nCol < this.m_nNumCols - 1; nCol++)
         {
            //
            // Try each available neighbor (going clock-wise from upper left) 
            // to see if it is available (bounds check).
            //
            
            //
            // Neighbor 1
            //
            
            nX = nRow - 1;
            nY = nCol - 1;
            
            if ((nX > -1) && (nY > -1) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 2
            //
            
            nX = nRow - 1;
            nY = nCol;
            
            if ((nX > -1) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 3
            //
            
            nX = nRow - 1;
            nY = nCol + 1;
            
            if ((nX > -1) && (nY < this.m_nNumCols) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 4
            //
            
            nX = nRow;
            nY = nCol + 1;
            
            if ((nY < this.m_nNumCols) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 5
            //
            
            nX = nRow + 1;
            nY = nCol + 1;
            
            if ((nX < this.m_nNumRows) && (nY < this.m_nNumCols) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 6
            //
            
            nX = nRow + 1;
            nY = nCol;
            
            if ((nX < this.m_nNumRows) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 7
            //
            
            nX = nRow + 1;
            nY = nCol - 1;
            
            if ((nX < this.m_nNumRows) && (nY > -1) && 
                (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }
            
            //
            // Neighbor 8
            //
            
            nX = nRow;
            nY = nCol - 1;
            
            if ((nY > -1) && 
               (m_aFrontBoardArea[nX][nY] == true))
            {
               nTempNeighborCount++;
            }

            //
            // Apply the Rules
            //
            
            if ((nTempNeighborCount == 3) && 
                (m_aFrontBoardArea[nRow][nCol] == false))
            {
               m_aBackBoardArea[nRow][nCol] = true;
            }
            else if ((nTempNeighborCount < 2) || (nTempNeighborCount > 3))
            {
               m_aBackBoardArea[nRow][nCol] = false;
            }
            else
            {
               // Exactly 2 or 3 neighbors - Live unchanged until the next generation.
               m_aBackBoardArea[nRow][nCol] = m_aFrontBoardArea[nRow][nCol];
            }
            
            nTempNeighborCount = 0;
         }
      }
      
      m_aTempBoardArea = m_aFrontBoardArea;
      m_aFrontBoardArea = m_aBackBoardArea;
      m_aBackBoardArea = m_aTempBoardArea;
      m_aTempBoardArea = null;
   }
   
   public boolean[][] GetFrontGameBoard()
   {
      return m_aFrontBoardArea;
   }
   
   public int GetNumZoneRows()
   {
      return m_nNumRows;
   }
   
   public int GetNumZoneCols()
   {
      return m_nNumCols;  
   }
}
