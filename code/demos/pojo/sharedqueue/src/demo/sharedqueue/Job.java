/*
@COPYRIGHT@
*/
package demo.sharedqueue;

import java.util.Random;

public class Job {

   private int duration;
   private String producer;
   private Worker consumer;
   private int type;
   private int state;
   private String id;
   private static final int STATE_READY = 0;
   private static final int STATE_PROCESSING = 1;
   private static final int STATE_COMPLETE = 2;
   private static final int STATE_ABORTED = 3;

   public Job(String producer, int id) {
      Random random = new Random();
      this.state = STATE_READY;
      this.consumer = null;
      this.producer = producer;
      this.duration = random.nextInt(3) + 3;
      this.type = random.nextInt(3) + 1;
      this.id = Integer.toString(id);
      while (this.id.length() < 3) {
         this.id = "0" + this.id;
      }
   }

   public void run(Worker consumer) {
      synchronized (this) {
         this.state = STATE_PROCESSING;
         this.consumer = consumer;
         try {
            Thread.sleep(duration * 1000L);
            this.state = STATE_COMPLETE;
         }
         catch (InterruptedException ie) {
            this.state = STATE_ABORTED;
         }
      }
   }

   public String toXml() {
      return "<job>" +
            "<id>" + id + "</id>" +
            "<type>" + type + "</type>" +
            "<state>" + state + "</state>" +
            "<producer>" + producer + "</producer>" +
            "<consumer>" + getConsumer() + "</consumer>" +
            "<duration>" + duration + "</duration>" +
            "</job>";
   }

   private String getConsumer() {
      return consumer == null ? "" : consumer.getName();
   }
}
