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
       implements Runnable {

   private String name;
   private int port;
   private Queue queue;
   private List jobs;
   private String nodeId;

   private int health = HEALTH_ALIVE;
   private static int HEALTH_ALIVE = 0;
   private static int HEALTH_DYING = 1;
   private static int HEALTH_DEAD = 2;

   private static final int MAX_LOAD = 10;

   public Worker(Queue queue, int port, String nodeId) {
      this.name = Queue.getHostName();
      this.port = port;
      this.queue = queue;
      this.nodeId = nodeId;
      jobs = Collections.synchronizedList(new LinkedList());
   }
   
   public String getNodeId() {
      return this.nodeId;
   }

   public String getName() {
      return "node: " + nodeId + " (" + name + ":" + port + ")";
   }

   public String toXml() {
      synchronized (jobs) {
         String data = "<worker><name>" + getName() + "</name><jobs>";
         ListIterator i = jobs.listIterator();
         while (i.hasNext()) {
            data += ((Job) i.next()).toXml();
         }
         data += "</jobs></worker>";
         return data;
      }
   }

   public boolean attemptKill() {
      if (HEALTH_DYING == health) {
         if (jobs.size() > 0) {
            queue.addJob((Job) jobs.remove(0));
         }
         setHealth((jobs.size() == 0) ? HEALTH_DEAD : HEALTH_DYING);
      }
      return HEALTH_DEAD == health;
   }
   
   public void markForDeath() {
      setHealth(HEALTH_DYING);
   }

   public void run() {
      while (HEALTH_DEAD != health) {
         if ((HEALTH_ALIVE == health) && (jobs.size() < MAX_LOAD)) {
            final Job job = queue.getJob();

            try {
               Thread.sleep(500);
            }
            catch (InterruptedException ie) {
               System.err.println(ie.getMessage());
            }

            synchronized (jobs) {
               jobs.add(job);
            }

            Thread processor = new Thread(
                     new Runnable() {
                        public void run() {
                           job.run(Worker.this);
                           synchronized (jobs) {
                              jobs.remove(job);
                           }
                           queue.log(job);
                        }
                     }
                  );
            processor.start();
         }
      }
   }

   private void setHealth(int health) {
      this.health = health;
   }
}
