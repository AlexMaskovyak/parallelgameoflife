import java.util.Comparator;


public class CellComparator implements Comparator {
   public int compare(Object arg0, Object arg1) {
      
      Cell cell0 = (Cell)arg0;
      Cell cell1 = (Cell)arg1;
      //
      // For now, only do column sorting
      //
      
      if (cell0.y < cell1.y)
      {
         return -1;
      }
      else if (cell0.y > cell1.y)
      {
         return 1;
      }
      else
      {
         return 0;
      }
   }

}
