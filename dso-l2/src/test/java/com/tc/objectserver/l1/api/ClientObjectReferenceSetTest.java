/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
    final ClientStateManagerImpl clientStateManager = new ClientStateManagerImpl();
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
      @Override
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
//  minus one because this thread can race past the incrementing thread above, current - 1 is known to have been put into set
      int current = currentObjectID.intValue() - 1;
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

    @Override
    public void notifyReferenceSetChanged() {
      count++;
      System.err.println("XXX Client Object Ref Set refreshed - " + count);
    }

    private int getCount() {
      return count;
    }

  }
}
