/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

public class BlockingQueueInterruptTakeTestClient extends ClientBase {

  private static final int  CAPACITY   = 100;

  private static final int  numOfLoops = 1000;

  private int               localCount;

  private transient Thread1 thread1;

  private volatile int      node       = -1;

  public BlockingQueueInterruptTakeTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new BlockingQueueInterruptTakeTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    BlockingQueue queue = toolkit.getBlockingQueue("test-blocking-queue", CAPACITY, null);
    ToolkitAtomicLong counter = toolkit.getAtomicLong("test-counter");
    node = getBarrierForAllClients().await();

    thread1 = new Thread1("Node " + node + " thread 1", queue, counter);
    thread1.start();
    Thread2 thread2 = new Thread2("Node " + node + " thread 2", queue);
    thread2.start();

    System.out.println("Node " + node + " : waiting for thread 2");

    thread2.join();

    // wait for all thread 2 to be finished
    getBarrierForAllClients().await();

    // wait for the queue to be empty
    synchronized (queue) {
      while (queue.size() > 0) {
        queue.wait(500);
      }
    }

    Thread.sleep(5000);

    System.out.println("Node " + node + " : stopping thread 1");

    thread1.stopLoop();
    thread1.interrupt();

    System.out.println("Node " + node + " : waiting for thread 1");
    thread1.join();

    // wait for all thread 1 to be finished
    node = getBarrierForAllClients().await();

    System.out.println(">>> Node " + node + " : Local count : " + localCount);

    if (0 == node) {
      System.out.println(">>> Total count : " + counter);

      // check how many items have been taken from the queue
      int expected = numOfLoops * getParticipantCount();
      System.out.println("Took " + counter + " from queue, expected " + expected);
      Assert.assertEquals(counter.intValue(), expected);
    }
  }

  public class Thread1 extends Thread {
    private volatile boolean        stop = false;
    private final BlockingQueue     queue;
    private final ToolkitAtomicLong counter;

    public Thread1(final String name, BlockingQueue queue, ToolkitAtomicLong counter) {
      super(name);
      this.queue = queue;
      this.counter = counter;
    }

    public void stopLoop() {
      this.stop = true;
    }

    @Override
    public void run() {
      while (!stop) {
        Object taken = null;
        try {
          // System.out.println("Node "+node+" - thread 1 : "+queue.size()+" - taking");
          taken = queue.take();
          System.out.println("Node " + node + " - thread 1 : " + queue.size() + " - took : " + taken);
        } catch (InterruptedException e) {
          System.out.println("Node " + node + " - thread 1 : InterruptedException");
        } finally {
          // System.out.println("Node "+node+" - thread 1 : checking taken state");
          if (taken != null) {
            synchronized (queue) {
              queue.notifyAll();
              System.out.println("Node " + node + " - thread 1 : updating counts - " + (++localCount) + ", "
                                 + (counter.incrementAndGet()));
            }
          }
        }
      }
      System.out.println("Node " + node + " - thread 1 : finished");
    }
  }

  public class Thread2 extends Thread {
    private final BlockingQueue queue;

    public Thread2(final String name, BlockingQueue queue) {
      super(name);
      this.queue = queue;
    }

    @Override
    public void run() {
      for (int i = 0; i < numOfLoops; i++) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          continue;
        }

        try {
          if (queue.size() >= 20) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              // do nothing
            }
          }
          Object o = new Date();
          // System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - putting : "+o);
          queue.put(o.toString());
          System.out.println("Node " + node + " - thread 2 : " + queue.size() + " - put : " + o);
          synchronized (queue) {
            // System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - checking queue size");
            if (0 == queue.size()) {
              System.out.println("Node " + node + " - thread 2 : " + queue.size() + " - interrupting thread 1");
              thread1.interrupt();
              // System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - interrupted thread 1");
            }
          }
        } catch (InterruptedException e) {
          System.out.println("Node " + node + " - thread 2 : InterruptedException");
        }
      }
      System.out.println("Node " + node + " - thread 2 : finished");
    }
  }
}
