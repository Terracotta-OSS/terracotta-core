/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkRejectedException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
* The distributed work queue. Implements a blocking queue, with an optional upper buffer limit.
*/
public class WorkQueue 
{
   private final BlockingQueue m_workQueue;

   public WorkQueue() 
   {
      m_workQueue = new LinkedBlockingQueue();
   }

   public WorkQueue(final int capacity) 
   {
      m_workQueue = new LinkedBlockingQueue(capacity);
   }

   public TCWorkItem getWork() 
   throws WorkException 
   {
      try 
      {
         return (TCWorkItem) m_workQueue.take(); // blocks if queue is empty
      } 
      catch (InterruptedException e) 
      {
         throw new WorkException(e);
      }
   }

   public void addWork(final WorkItem workItem) 
   throws WorkException 
   {
      try 
      {
         m_workQueue.put(workItem); // blocks if queue is full
      } 
      catch (InterruptedException e) 
      {
         WorkRejectedException we = new WorkRejectedException(e.getMessage());
         ((TCWorkItem)workItem).setStatus(WorkEvent.WORK_REJECTED, we);
         throw we;
      }
   }
}
