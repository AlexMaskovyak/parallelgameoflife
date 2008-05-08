import edu.rit.pj.Comm;

public class GameOfLife 
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

      long startTime, endTime;
      long nanoToMilliConvert = 1000000;

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
               nNumAliveCells, strInputFilename);
         
         startTime = System.nanoTime();

         master.DistributeDataToWorkers();
         master.DoWork(nNumIterationsToRun);

         endTime = System.nanoTime();
         System.out.format("Processor %d: %d milliseconds elapsed\n", nMyProcessorRank,
               (endTime - startTime) / nanoToMilliConvert);
      }
      
      //
      // Worker
      //
      
      else
      {
         WorkerProcessor worker = new WorkerProcessor(
               nMyProcessorRank, nNumProcessors, nNumGridRows, nNumGridCols);
         
         startTime = System.nanoTime();
         
         worker.ReceiveDataFromMaster();
         worker.DoWork(nNumIterationsToRun);
         
         endTime = System.nanoTime();
         System.out.format("Processor %d: %d milliseconds elapsed\n", nMyProcessorRank,
               (endTime - startTime) / nanoToMilliConvert);
      }


   }
}
