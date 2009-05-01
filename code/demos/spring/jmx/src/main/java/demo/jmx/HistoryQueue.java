/*
@COPYRIGHT@
*/
package demo.jmx;

import java.util.LinkedList;
import java.util.List;

/**
* Bounded queue collection
*/
public class HistoryQueue 
implements IHistory 
{
   private static final int DEFAULT_INTERVAL = 30;

   private final long granularity;
   private final int capacity;
   private final List buffer = new LinkedList();

   private boolean enabled = true;

   public HistoryQueue() 
   {
      this(DEFAULT_INTERVAL, 24 * 60 * 60 / DEFAULT_INTERVAL);  // default 24h
   }

   public HistoryQueue(int granularity, int capacity) 
   {
      this.granularity = granularity * 1000;
      this.capacity = capacity;
   }

   // JMX access 

   public String[] getHistory() 
   {
      synchronized(this.buffer) 
      {
         String[] history = new String[this.buffer.size()];
         for(int i = 0; i<history.length; i++) 
         {
            HistoryData data = (HistoryData) this.buffer.get(i);
            history[i] = data.toString();
         }
         return history;
      }
   }

   public boolean getEnabled() 
   {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) 
   {
      synchronized(this) 
      {
         this.enabled = enabled;
      }
   }

   public void reset() 
   {
      synchronized(this.buffer) 
      {
         this.buffer.clear();
      }
   }

   public void updateHistory(int duration, String error) 
   {
      if(this.enabled) 
      {
         synchronized(this) 
         {
            HistoryData historyData = getHistoryData(System.currentTimeMillis());
            historyData.update(duration, error);
         }
      }
   }

   private HistoryData getHistoryData(long time) 
   {
      HistoryData data = (HistoryData) peek();

      long intervalStart = time - (time % granularity);

      if(data == null) 
      {
         data = new HistoryData(intervalStart, 0);
         add(data);
      } 
      else 
      {
         if(time - data.getIntervalStart() > granularity) 
         {
            data = new HistoryData(intervalStart, 0);
            add(data);
         }
      }
      return data;
   }

   /**
   * Adds new object to the queue and returns object pushed out 
   * of the queue as a result of add or null if nothing was 
   * pushed out (max capacity not reached yet).
   */
   private Object add(Object o) 
   {
      Object removed = null;
      if(this.buffer.size() >= this.capacity) removed = this.buffer.remove(0);
      this.buffer.add(o);
      return removed;
   }

   private Object peek() 
   {
      final int size = this.buffer.size();
      return size == 0 ? null : this.buffer.get(size - 1);
   }
}
