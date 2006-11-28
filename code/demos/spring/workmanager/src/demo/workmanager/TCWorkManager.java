/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.Work;
import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * WorkerManager bean, has methods for scheduling new work sets and waiting for and retrieving completed work.
 */
public class TCWorkManager 
implements WorkManager 
{ 
   private transient final WorkQueue m_queue;

   public TCWorkManager(final WorkQueue queue) 
   {
      m_queue = queue;
   }

   public WorkItem schedule(final Work work) 
   throws WorkException 
   {
      WorkItem workItem = new TCWorkItem(work, null);
      m_queue.addWork(workItem);
      return workItem;
   }

   public WorkItem schedule(final Work work, final WorkListener listener) 
   throws WorkException 
   {
      WorkItem workItem = new TCWorkItem(work, listener);
      m_queue.addWork(workItem);
      return workItem;
   }

   public boolean waitForAll(final Collection workItems, final long timeout) 
   {
      long start = System.currentTimeMillis();
      do 
      {
         synchronized (this) 
         {
            boolean isAllCompleted = true;
            for (Iterator it = workItems.iterator(); it.hasNext() && isAllCompleted;) 
            {
               int status = ((WorkItem) it.next()).getStatus();
               isAllCompleted = status == WorkEvent.WORK_COMPLETED || 
               status == WorkEvent.WORK_REJECTED;
            }
            if (isAllCompleted) return true; 
            if (timeout == IMMEDIATE) return false; 
            if (timeout == INDEFINITE) continue; 
         }
      } 
      while ((System.currentTimeMillis() - start) < timeout);
      return false;
   }

   public Collection waitForAny(final Collection workItems, final long timeout) 
   {
      long start = System.currentTimeMillis();
      do 
      {
         synchronized (this) 
         {
            Collection completed = new ArrayList();
            for (Iterator it = workItems.iterator(); it.hasNext();) 
            {
               WorkItem workItem = (WorkItem) it.next();
               if (workItem.getStatus() == WorkEvent.WORK_COMPLETED || workItem.getStatus() == WorkEvent.WORK_REJECTED) 
               completed.add(workItem);
            }
            if (!completed.isEmpty()) return completed;
         }
         if (timeout == IMMEDIATE) return Collections.EMPTY_LIST; 
         if (timeout == INDEFINITE) continue; 
      } 
      while ((System.currentTimeMillis() - start) < timeout);
      return Collections.EMPTY_LIST;
   }
}