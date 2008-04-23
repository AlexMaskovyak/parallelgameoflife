// ----------------------------------------------------------------------------
// A Lightweight Class for representing Dead and Alive Cells.
// ----------------------------------------------------------------------------

// Hey just a test

public class Cell implements java.io.Serializable
{
   public static enum LifeState
   { 
      Alive,
      Dead
   };
   
   public LifeState m_CurrentState;
   public int m_nX, m_nY;
   
   public Cell()
   {
      this.m_CurrentState = LifeState.Dead;
      this.m_nX = 0;
      this.m_nY = 0;
   }
   
   public Cell(LifeState currentState, int nX, int nY)
   {
      this.m_CurrentState = LifeState.Dead;
      this.m_nX = nX;
      this.m_nY = nY;
   }
}
