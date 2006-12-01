/*
@COPYRIGHT@
*/
package demo.events;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Simple event processor, keeps track of the last {@link MAX_EVENTS} Spring application events of type
 * {@link MessageEvent}.
 */
public class EventProcessor 
   implements ApplicationListener 
{
   private static final int MAX_EVENTS = 15;
   private transient List events = new ArrayList();

   public void onApplicationEvent(ApplicationEvent event) 
   {
      if (event instanceof MessageEvent) 
      {
         synchronized (events) 
         {
            events.add(0, event);
            if (events.size() > MAX_EVENTS) events.remove(events.size() - 1);
         }
      }
   }

   public List getEvents() 
   {
      synchronized(events) 
      {
         return new ArrayList(events);
      }
   }
}
