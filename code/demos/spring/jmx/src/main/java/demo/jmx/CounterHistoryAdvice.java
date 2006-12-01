/*
@COPYRIGHT@
*/
package demo.jmx;

import java.util.HashMap;
import java.util.Map;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
* Advice bean used to capture performance metrics 
* per time interval and to expose collected data trough JMX 
*/
public class CounterHistoryAdvice 
implements MethodInterceptor 
{
   private Map queues = new HashMap();

   public void setQueues(Map queues) 
   {
      this.queues = queues;
   }

   /**
   * Advice method updating perfrormance metrics
   * @see org.aopalliance.intercept.MethodInterceptor#invoke(MethodInvocation invocation)
   */
   public Object invoke(MethodInvocation invocation) 
   throws Throwable 
   {
      String error = null;
      try 
      {
         return invocation.proceed();

      } 
      catch(Throwable t) 
      {
         error = t.toString();
         throw t;

      } 
      finally 
      {
         String name = ((ICounter) invocation.getThis()).getName();
         HistoryQueue historyQueue = (HistoryQueue) queues.get(name);
         if(historyQueue!=null) historyQueue.updateHistory(0, error);
      }
   }
}
