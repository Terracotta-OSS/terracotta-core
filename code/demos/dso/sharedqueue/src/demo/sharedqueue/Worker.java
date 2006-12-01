/*
@COPYRIGHT@
*/
package demo.sharedqueue;

import java.util.Date;
import java.util.Collections;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class Worker 
   implements Runnable 
{
   private static int HEALTH_ALIVE = 0;
   private static int HEALTH_DYING = 1;
   private static int HEALTH_DEAD  = 2;
   
   private String name;
   private int port;
   private Queue queue;
   private List jobs;

   public Worker(Queue queue, int port) 
   {
      this.name  = Queue.getHostName();
      this.port  = port;
      this.queue = queue;
      jobs       = Collections.synchronizedList(new LinkedList());
   }

   public String getName()
   {
      return name + " " + port;
   }
   
   public String toXml()
   {
      synchronized(jobs)
      {
         String data    = "<worker><name>" + getName() + "</name><jobs>";
         ListIterator i = jobs.listIterator();
         while (i.hasNext()) 
            data += ((Job)i.next()).toXml();
         data += "</jobs></worker>";
         return data;
      }
   }
   
   private long lastBeat = (new Date()).getTime();
   
   public void keepAlive()
   {
      lastBeat = (new Date()).getTime();
   }

   private int health = HEALTH_ALIVE;
   
   private void setHealth(int health)
   {
      this.health = health;
   }
   
   public boolean attemptKill()
   {
      long currBeat  = (new Date()).getTime();
      long elapsed   = currBeat - lastBeat;
      if (HEALTH_ALIVE == health)
         setHealth((elapsed > 2500) ? HEALTH_DYING : HEALTH_ALIVE);
      else if (HEALTH_DYING == health)
      {
         if (jobs.size() > 0) queue.addJob((Job)jobs.remove(0));
         setHealth((jobs.size() == 0) ? HEALTH_DEAD : HEALTH_DYING);
      }
      return HEALTH_DEAD == health;
   }
   
   private final static int MAX_LOAD = 10;

   public void run() 
   {
      while (HEALTH_DEAD != health)
      {
         if ((HEALTH_ALIVE == health) && (jobs.size() < MAX_LOAD))
         {
            final Job job = queue.getJob();
            
            try { Thread.sleep(500); }
            catch (InterruptedException ie) { System.err.println(ie.getMessage()); }
            
            synchronized(jobs) { jobs.add(job); }
            
            Thread processor = new Thread(
               new Runnable() 
               { 
                  public void run() 
                  { 
                     job.run(Worker.this); 
                     synchronized(jobs) { jobs.remove(job); }
                     queue.log(job);
                   } 
               }
            );
            processor.start();
         }
      }
   }
}
