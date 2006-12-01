/*
@COPYRIGHT@
*/
package demo.jmx;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
* Basic container to hold performance metrics for time interval
*/
public class HistoryData 
{
   private final long intervalStart;

   private int counter;

   public HistoryData(long intervalStart, int counter) 
   {
      this.intervalStart = intervalStart;
      this.counter = counter;
   }

   public long getIntervalStart() 
   {
      return this.intervalStart;
   }

   public Date getTime() 
   {
      return new Date(intervalStart);
   }

   public int getCounter() 
   {
      return this.counter;
   }

   public void incrementCounter() 
   {
      this.counter++;
   }

   public void update(int duration, String error) 
   {
      this.counter++;
   }

   public String toString() 
   {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return df.format(new Date(intervalStart))+" : "+counter;
   }
}
