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
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

public class ClientObjectReferenceSetConsistencyTest extends TCTestCase {

  public void testBasic() throws Exception {

    final AtomicReference<Exception> failure = new AtomicReference<Exception>();
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");

    final AtomicLong currentObjectID = new AtomicLong(-1);
    final ClientStateManagerImpl clientStateManager = new ClientStateManagerImpl();
    final NodeID node = new ClientID(1);
    final CyclicBarrier coordinator = new CyclicBarrier(2);
    clientStateManager.startupNode(node);
    final ClientObjectReferenceSet clientObjRefSet = new ClientObjectReferenceSet(clientStateManager);
    // Add some objects to the reference set so that the refresh
    for (int i = 0; i < 2000; i++) {
      clientStateManager.addReference(node, new ObjectID(currentObjectID.incrementAndGet()));
    }

    Thread refreshThread = new Thread(new Runnable() {

      @Override
      public void run() {
        for (int i = 0; i < 100; i++) {
          try {
            // Try to Refresh the client Object Reference set in parallel to adding reference.
            ThreadUtil.reallySleep(1000);
            coordinator.await();
            clientObjRefSet.contains(new ObjectID(currentObjectID.get()));
            coordinator.await();

          } catch (Exception e) {
            System.out.println("XXX Got Exception in refresh thread" + e);
            failure.set(e);
          }
        }
      }
    });
    refreshThread.start();

    for (int i = 0; i < 100; i++) {
      try {
        System.out.println("XXX Iteration " + i);
        ThreadUtil.reallySleep(1000);
        coordinator.await();
        clientStateManager.addReference(node, new ObjectID(currentObjectID.incrementAndGet()));
        Assert.assertTrue(clientObjRefSet.contains(new ObjectID(currentObjectID.get())));
        coordinator.await();

      } catch (Exception e) {
        System.out.println("XXX Got Exception in test thread" + e);
        failure.set(e);
      }
    }
    Assert.assertNull(failure.get());
  }
}
