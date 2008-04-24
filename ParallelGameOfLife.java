import edu.rit.pj.Comm;

public class ParallelGameOfLife 
{
   public static void main(String[] args) throws Exception
   {
      //
      // Initialize Parallel Java Communication Framework.
      //
      
      Comm.init(args);
      Comm oCommWorld = Comm.world();
      int nNumProcessors = oCommWorld.size();
      int nMyProcessorRank = oCommWorld.rank();
      
      int nNumGridRows = Integer.parseInt(args[0]);
      int nNumGridCols = Integer.parseInt(args[1]);
      int nNumAliveCells = Integer.parseInt(args[2]);
      int nNumIterationsToRun = Integer.parseInt(args[3]);
      String strInputFilename = args[4];
      
      //
      // Master
      //
      
      if (nMyProcessorRank == 0)
      {
         MasterProcessor master = new MasterProcessor(
               nMyProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols, 
               nNumAliveCells, nNumIterationsToRun, strInputFilename);
         master.DistributeDataToWorkers();
         master.DoWork();
      }
      
      //
      // Worker
      //
      
      else
      {
         WorkerProcessor worker = new WorkerProcessor(
               nMyProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols, nNumIterationsToRun);
         worker.ReceiveDataFromMaster();
         worker.DoWork();
      }
   }
}
