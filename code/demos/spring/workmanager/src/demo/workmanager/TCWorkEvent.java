/**
@COPYRIGHT@
*/
package demo.workmanager;

import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkItem;

/**
* Work event, is sent to the registered WorkListener when the status for the work has been changed.
*/
public class TCWorkEvent 
implements WorkEvent 
{
   private int           m_type;
   private WorkItem      m_workItem;
   private WorkException m_exception;

   public TCWorkEvent(int type, WorkItem item, WorkException exception) 
   {
      m_type = type;
      m_workItem = item;
      m_exception = exception;
   }

   public int getType() 
   {
      return m_type;
   }

   public WorkItem getWorkItem() 
   {
      return m_workItem;
   }

   public WorkException getException() 
   {
      return m_exception;
   }
}
