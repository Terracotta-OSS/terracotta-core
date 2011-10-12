/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ClientObjectReferenceSetTest extends TCTestCase {

  public void testBasic() {

    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");

    final AtomicLong currentObjectID = new AtomicLong(-1);
    final AtomicBoolean stop = new AtomicBoolean(false);
    final TCLogger logger = TCLogging.getLogger(ClientObjectReferenceSetTest.class);
    final ClientStateManagerImpl clientStateManager = new ClientStateManagerImpl(logger);
    final NodeID node = new ClientID(1);
    final Random r = new Random();

    clientStateManager.startupNode(node);
    ClientObjectReferenceSet clientObjRefSet = new ClientObjectReferenceSet(clientStateManager);
    MyListener listener = new MyListener();
    clientObjRefSet.addReferenceSetChangeListener(listener);

    for (int i = 0; i < 1000; i++) {
      clientStateManager.addReference(node, new ObjectID(currentObjectID.incrementAndGet()));
    }

    new Thread(new Runnable() {
      public void run() {
        while (!stop.get()) {
          ThreadUtil.reallySleep(r.nextInt(100));
          clientStateManager.addReference(node, new ObjectID(currentObjectID.incrementAndGet()));
        }
      }
    }).start();

    System.err.println("XXX ServerMap Eviction continuously running");
    for (int i = 0; i < 10; i++) {
      ThreadUtil.reallySleep(r.nextInt(50));
      int current = currentObjectID.intValue();
      int id = r.nextInt(current);
      System.err.println("XXX CurrentObjectIDSeq=" + current + "; toCheck=" + id + ", " + current);
      Assert.eval(clientObjRefSet.contains(new ObjectID(id)));
      Assert.eval(clientObjRefSet.contains(new ObjectID(current)));
    }

    System.err.println("XXX ServerMap Eviction running intermittently");
    int count = 10;
    for (int i = 0; i < count; i++) {
      ThreadUtil.reallySleep(TimeUnit.NANOSECONDS.toMillis(ClientObjectReferenceSet.MONITOR_INTERVAL_NANO) + 1000);
      while (clientStateManager.getObjectReferenceAddRegisteredListeners().length != 0) {
        System.err.println("XXX waiting for ref listener to be 0");
        ThreadUtil.reallySleep(1000);
      }

      int current = currentObjectID.intValue();
      int id = r.nextInt(current);
      System.err.println("XXX CurrentObjectIDSeq=" + current + "; toCheck=" + id + ", " + current);
      Assert.eval(clientObjRefSet.contains(new ObjectID(id)));
      Assert.eval(clientObjRefSet.contains(new ObjectID(current)));
      while (clientStateManager.getObjectReferenceAddRegisteredListeners().length != 1) {
        System.err.println("XXX waiting for ref listener to be 1");
        ThreadUtil.reallySleep(1000);
      }
    }

    Assert.eval(listener.getCount() >= count);
    stop.set(true);
  }

  private class MyListener implements ClientObjectReferenceSetChangedListener {

    private volatile int count;

    public void notifyReferenceSetChanged() {
      count++;
      System.err.println("XXX Client Object Ref Set refreshed - " + count);
    }

    private int getCount() {
      return count;
    }

  }
}
