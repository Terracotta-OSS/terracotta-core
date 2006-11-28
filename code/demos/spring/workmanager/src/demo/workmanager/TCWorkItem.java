/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.Work;
import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;

/**
* The work item, holds the work to be executed, status of the progress and the future result.
* FIXME implement RemoteWorkItem - is it necessary??
*/
public class TCWorkItem 
implements WorkItem 
{
   private int                m_status;
   private final Work         m_work;
   private final WorkListener m_workListener;

   public TCWorkItem(final Work work, final WorkListener workListener) 
   {
      m_work = work;
      m_status = WorkEvent.WORK_ACCEPTED;
      m_workListener = workListener;
   }

   public void setStatus(final int status, final WorkException exception) 
   {
      synchronized (this) 
      {
         m_status = status;
      }
      if (m_workListener != null) 
      {
         switch (status) 
         {
            case WorkEvent.WORK_ACCEPTED:
            m_workListener.workAccepted(new TCWorkEvent(WorkEvent.WORK_ACCEPTED, this, exception));
            break;
            case WorkEvent.WORK_REJECTED:
            m_workListener.workRejected(new TCWorkEvent(WorkEvent.WORK_REJECTED, this, exception));
            break;
            case WorkEvent.WORK_STARTED:
            m_workListener.workStarted(new TCWorkEvent(WorkEvent.WORK_STARTED, this, exception));
            break;
            case WorkEvent.WORK_COMPLETED:
            m_workListener.workCompleted(new TCWorkEvent(WorkEvent.WORK_COMPLETED, this, exception));
            break;
         }
      }
   }

   public Work getResult() 
   {
      return m_work;
   }

   public int getStatus() 
   {
      return m_status;
   }

   public int compareTo(Object arg0) 
   {
      return 0;
   }

   public String toString() 
   {
      String status = "";
      switch (m_status) 
      {
         case WorkEvent.WORK_ACCEPTED:
         status = "WORK_ACCEPTED";
         break;
         case WorkEvent.WORK_COMPLETED:
         status = "WORK_COMPLETED";
         break;
         case WorkEvent.WORK_REJECTED:
         status = "WORK_REJECTED";
         break;
         case WorkEvent.WORK_STARTED:
         status = "WORK_STARTED";
         break;
      }
      return m_work.toString() + ":" + status;
   }
}
