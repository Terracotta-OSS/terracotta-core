/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.Work;
import commonj.work.WorkItem;
import commonj.work.WorkManager;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main 
{
   public static void main(String[] args) 
   throws Exception 
   {
      Thread.currentThread().sleep(5000);

      ApplicationContext ctx = new ClassPathXmlApplicationContext("work-manager.xml");
      boolean isWorkManager = args[0].equals("workManager");
      if (isWorkManager) 
      {
         System.out.println("-- starting workmanager...");

         // get the work manager from the application context
         TCWorkManager workManager = (TCWorkManager) ctx.getBean("workManager");

         long l1 = System.currentTimeMillis();

         Set pendingWork = new HashSet();
         for (int workNr = 0; workNr < 1000; workNr++) 
         {
            System.out.print("\rscheduling work "+workNr+"      ");

            // schedule work
            WorkItem workItem = workManager.schedule(new NumberedWork(workNr));

            // collect the pending work
            pendingWork.add(workItem);
         }
         System.out.println("\rall sheduled                    ");

         System.out.println("-- waiting for all work to be completed...");
         // wait until all work is done
         workManager.waitForAll(pendingWork, WorkManager.INDEFINITE);

         System.out.println("recieved completed work: " + pendingWork);
         long l2 = System.currentTimeMillis();
         System.out.println((l2 - l1)/1000f + "seconds");

      } 
      else 
      {      
         // get the worker from the application context
         final Worker worker = (Worker) ctx.getBean("worker");

         // starting worker
         System.out.println("-- starting worker...");
         System.out.println("-- kill it with ^C when finished");
         worker.start();
      }
   }
}

/**
* Class that adds a work number to the work.
*/
class NumberedWork 
implements Work 
{
   protected final int m_workNumber;

   public NumberedWork(int workNumber) 
   {
      m_workNumber = workNumber;
   }

   public int getWorkNumber() 
   {
      return m_workNumber;
   }

   public void release() 
   {
      //
   }

   public boolean isDaemon() 
   {
      return false;
   }

   public String toString() 
   {
      return "Work[" + m_workNumber + "]";
   }

   public void run() 
   {
      System.out.println("executing work nr " + m_workNumber);
   }
}
