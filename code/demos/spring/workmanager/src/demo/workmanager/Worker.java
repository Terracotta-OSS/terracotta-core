/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.Work;
import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkRejectedException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Worker bean, recieves a work set from the application context. It grabs the next pending work, and executes it.
 */
public class Worker 
{
   private transient final WorkQueue       m_queue;
   private transient final ExecutorService m_threadPool = Executors.newCachedThreadPool();
   private volatile boolean                m_isRunning  = true;

   public Worker(final WorkQueue queue) 
   {
      m_queue = queue;
   }

   public void start() 
   throws WorkException 
   {
      while (m_isRunning) 
      {
         final TCWorkItem workItem = m_queue.getWork();
         m_threadPool.execute(new Runnable() 
         {
            public void run() 
            {
               try 
               {
                  Work work = workItem.getResult();
                  workItem.setStatus(WorkEvent.WORK_STARTED, null);
                  work.run();
                  workItem.setStatus(WorkEvent.WORK_COMPLETED, null);
               } 
               catch (Throwable e) 
               {
                  workItem.setStatus(WorkEvent.WORK_REJECTED, new WorkRejectedException(e.getMessage()));
               }
            }
         });
      }
   }

   public void stop() 
   {
      m_threadPool.shutdown();
      m_isRunning = false;
   }
}
