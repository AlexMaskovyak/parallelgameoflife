import edu.rit.pj.Comm;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Date;
import java.text.SimpleDateFormat;

public abstract class Processor 
{
   protected static Comm m_CommWorld;
   protected int m_nNumGridRows;
   protected int m_nNumGridCols;
   protected int m_nNumAliveCells;
   protected int m_nNumIterationsToRun;
   protected Cell[][] m_aGlobalGameBoard;
   
   protected int m_nProcessorRank;
   protected int m_nNumProcessors;
   private FileWriter m_LogFileWriter;
   
   public abstract void DoWork();
   
   //
   // Since it will be difficult to debug communications on multiple
   // processors, it will be very useful to log data at any given time.
   //
   
   protected void LogMessage(String strMessage)
   {
      try
      {
         String strFilename = "Log/Processor_" + this.m_nProcessorRank + ".txt";
         m_LogFileWriter = new FileWriter(strFilename, true);
      }
      catch (IOException e)
      {
         return;
      }
      
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss a zzz");
      Date dCurrentTime = new Date();
      String dateString = formatter.format(dCurrentTime);
      
      try 
      {
         m_LogFileWriter.write(dateString + ": " + strMessage + "\n");
         m_LogFileWriter.close();
      }
      catch (IOException e)
      {
         return;
      }      
   }
}
