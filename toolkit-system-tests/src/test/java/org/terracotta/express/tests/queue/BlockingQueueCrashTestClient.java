/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

public class BlockingQueueCrashTestClient extends ClientBase {
  private final long            upbound = 1500;

  private BlockingQueue<String> bqueue1;
  private BlockingQueue<String> bqueue2;
  private ToolkitAtomicLong     eventIndex;

  public BlockingQueueCrashTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    int index = getBarrierForAllClients().await();
    eventIndex = toolkit.getAtomicLong("eventId");
    bqueue1 = toolkit.getBlockingQueue("bqueue1", null);
    bqueue2 = toolkit.getBlockingQueue("bqueue2", null);
    ToolkitAtomicLong stopTest = toolkit.getAtomicLong("stopTest");
    Random r = new Random();
    String node;

    while (stopTest.intValue() == 0) {
      index = getBarrierForAllClients().await();

      if (index < 6) {
        node = doPut();
        System.err.println("*** Doing put id=" + node + " by thread= " + index);
      } else {
        node = doPass();
        System.err.println("*** Doing pass id=" + node + " by thread= " + index);
      }
      // ended when total nodes exceed
      if (eventIndex.intValue() >= upbound) break;

      Thread.sleep(r.nextInt(20));
      if (eventIndex.intValue() < upbound) {
        stopTest.incrementAndGet();
      }
      getBarrierForAllClients().await();

    }
    index = getBarrierForAllClients().await();

    // verify
    if (index == 0) {
      System.err.println("*** Start verification");
      // verify size
      Assert.assertTrue("Wrong event count", eventIndex.intValue() == bqueue1.size() + bqueue2.size());

      // verify no event lost
      int lastItem;
      synchronized (eventIndex) {
        lastItem = eventIndex.intValue();
      }
      int id = 1;
      while (id <= lastItem) {
        String event = bqueue2.peek();
        if ((event != null) && event.equals(getEventForId(id))) {
          bqueue2.take();
        } else {
          boolean found = false;
          // searching for the event
          Iterator it = bqueue2.iterator();
          while (it.hasNext()) {
            event = (String) it.next();
            if (event.equals(getEventForId(id))) {
              found = true;
              bqueue2.remove(event);
              break;
            }
          }
          if (!found) {
            it = bqueue1.iterator();
            while (it.hasNext()) {
              event = (String) it.next();
              if (event.equals(getEventForId(id))) {
                found = true;
                bqueue1.remove(event);
                break;
              }
            }
          }

          Assert.assertTrue("Event " + id + " not found", found);
        }
        ++id;
        if (id % 10 == 0) System.err.println("*** Verify id=" + id);
      } // while

      Assert.assertTrue("Duplicate events found", 0 == (bqueue1.size() + bqueue2.size()));

      System.err.println("*** Verification Successful");
    }

    index = getBarrierForAllClients().await();
  }

  private String doPut() throws Exception {
    long eventId = eventIndex.incrementAndGet();
    String event = getEventForId(eventId);
    bqueue1.put(event);
    return (event);
  }

  private String getEventForId(long id) {
    return "event-" + id;
  }

  private String doPass() throws Exception {
    String node;
    node = bqueue1.take();
    bqueue2.put(node);
    return (node);
  }

}
