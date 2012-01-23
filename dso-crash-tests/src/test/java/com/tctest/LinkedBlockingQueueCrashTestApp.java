/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueCrashTestApp extends AbstractTransparentApp {
  int                                          upbound          = 2000;
  long                                         MaxRuntimeMillis = 5 * 60 * 1000 + 30000;

  private final CyclicBarrier                  barrier          = new CyclicBarrier(getParticipantCount());
  private final LinkedBlockingQueue<EventNode> lbqueue1         = new LinkedBlockingQueue<EventNode>();
  private final LinkedBlockingQueue<EventNode> lbqueue2         = new LinkedBlockingQueue<EventNode>();
  private final EventNode                      eventIndex       = new EventNode(0, "test");
  private final GetController                  controller       = new GetController(getParticipantCount());

  public LinkedBlockingQueueCrashTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      int index = barrier.await();
      testBlockingQueue(index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = LinkedBlockingQueueCrashTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + EventNode.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + GetController.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("lbqueue1", "lbqueue1");
    spec.addRoot("lbqueue2", "lbqueue2");
    spec.addRoot("eventIndex", "eventIndex");
    spec.addRoot("controller", "controller");
  }

  private void testBlockingQueue(int index) throws Exception {
    EventNode node = null;
    Random r = new Random();
    long endTime = System.currentTimeMillis() + MaxRuntimeMillis;

    while (true) {

      // avoid all are getter
      boolean doPut;
      if (r.nextBoolean()) {
        doPut = true;
      } else {
        synchronized (controller) {
          doPut = controller.canDoGet() ? false : true;
          if (!doPut) controller.incGetter();
        }
      }

      if (doPut) {
        node = doPut();
        System.err.println("*** Doing put id=" + node.getId() + " by thread= " + index + ", client: "
                           + ManagerUtil.getClientID());
      } else {
        node = doPass();
        System.err.println("*** Doing pass id=" + node.getId() + " by thread= " + index + ", client: "
                           + ManagerUtil.getClientID());
        controller.decGetter();
      }
      // ended when total nodes exceed
      if (node.getId() >= upbound) controller.ending();
      // limited by time too
      if (System.currentTimeMillis() >= endTime) controller.ending();

      if (controller.canQuit()) break;

      Thread.sleep(r.nextInt(20));
    }
    index = barrier.await();

    // verify
    if (index == 0) {
      System.err.println("*** Start verification");
      // verify size
      Assert.assertTrue("Wrong event count", eventIndex.getId() == lbqueue1.size() + lbqueue2.size());

      // verify no event lost
      int lastItem;
      synchronized (eventIndex) {
        lastItem = eventIndex.getId();
        eventIndex.setId(0);
      }
      int id = 0;
      while (id < lastItem) {
        EventNode event;
        event = lbqueue2.peek();
        if ((event != null) && (id == event.getId())) {
          lbqueue2.take();
        } else {
          boolean found = false;
          // searching for the event
          Iterator it = lbqueue2.iterator();
          while (it.hasNext()) {
            event = (EventNode) it.next();
            if (id == event.getId()) {
              found = true;
              lbqueue2.remove(event);
              break;
            }
          }
          if (!found) {
            it = lbqueue1.iterator();
            while (it.hasNext()) {
              event = (EventNode) it.next();
              if (id == event.getId()) {
                found = true;
                lbqueue1.remove(event);
                break;
              }
            }
          }

          Assert.assertTrue("Event " + id + " not found", found);
        }
        ++id;
        if (id % 10 == 0) System.err.println("*** Verify id=" + id);
      } // while

      Assert.assertTrue("Duplicate events found", 0 == (lbqueue1.size() + lbqueue2.size()));

      System.err.println("*** Verification Successful");
    }

    index = barrier.await();
  }

  private EventNode doPut() throws Exception {
    EventNode node;
    synchronized (eventIndex) {
      node = eventIndex.produce();
    }
    lbqueue1.put(node);
    return (node);
  }

  private EventNode doPass() throws Exception {
    EventNode node;
    node = lbqueue1.take();
    lbqueue2.put(node);
    return (node);
  }

  private static class EventNode {
    String name;
    int    id;

    public EventNode produce() {
      EventNode node;
      node = new EventNode(getId(), "Event" + getId());
      setId(getId() + 1);
      // System.out.println("*** Produce id=" + node.getId());
      return (node);
    }

    public EventNode(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public int getId() {
      return (id);
    }

    public void setId(int id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static class GetController {
    private final int participants;
    private int       getters;
    private boolean   ending = false;

    public GetController(int participant) {
      this.participants = participant;
      getters = 0;
    }

    public synchronized void incGetter() throws Exception {
      ++getters;
      System.out.println("*** incGetter " + getters);
      Assert.assertTrue("Stuck in every node is a getter", getters < participants);
    }

    public synchronized void decGetter() throws Exception {
      --getters;
      System.out.println("*** decGetter " + getters);
      Assert.assertTrue("Negative number of getter", getters >= 0);
    }

    public synchronized int nGetters() {
      return (getters);
    }

    public synchronized boolean canDoGet() {
      // no more getter if ending the test
      if (ending) return (false);
      else return (getters < (participants - 1));
    }

    public synchronized void clean() {
      getters = 0;
    }

    public synchronized void ending() {
      ending = true;
    }

    public synchronized boolean canQuit() {
      if (ending && (getters == 0)) return true;
      else return false;
    }
  }

}

