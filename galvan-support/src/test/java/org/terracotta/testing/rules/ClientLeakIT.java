/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.rules;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;


/**
 * This test is similar to BasicExternalClusterClassRuleIT, in that it uses the class rule to perform a basic test.
 * 
 * In this case, the basic test is watching how Galvan handles interaction with a basic active-passive cluster.
 */
// XXX: Currently ignored since this test depends on restartability of the server, which now requires a persistence service
//  to be plugged in (and there isn't one available, in open source).
public class ClientLeakIT {
  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).build(); //logConfigExtensionResourceName("custom-logback-ext.xml").build();

  /**
   * This will ensure that a fail-over correctly happens.
   */
  @Test
  public void testClientLeakIsCleaned() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    Connection leak = CLUSTER.newConnection();
    CLUSTER.getClusterControl().terminateActive();
    Thread reconnect = lookForConnectionEstablisher();
    while (reconnect == null) {
      Thread.sleep(1000);
      reconnect = lookForConnectionEstablisher();
    }
    leak = null;
    while (reconnect.isAlive()) {
      System.out.println("reconnect thread " + reconnect.getName() + " is alive " + reconnect.isAlive());
      System.gc();
      Thread.sleep(1000);
    }
  }

  @Test
  public void testConnectionEstablisherDiesAfterJob() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    Connection leak = CLUSTER.newConnection();
    CLUSTER.getClusterControl().terminateActive();
    Thread reconnect = lookForConnectionEstablisher();
    while (reconnect == null) {
      Thread.sleep(1000);
      reconnect = lookForConnectionEstablisher();
    }
    CLUSTER.getClusterControl().startOneServer();
    CLUSTER.getClusterControl().waitForActive();
    while (reconnect.isAlive()) {
      System.out.println("reconnect thread " + reconnect.getName() + " is alive " + reconnect.isAlive());
      System.gc();
      Thread.sleep(1000);
    }
  }
  
  @Test
  public void testConnectionMakerDiesWithNoRef() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().terminateAllServers();
    Thread target = Thread.currentThread();
    new Thread(()->{
      try {
        while (lookForConnectionMaker() == null) {
          Thread.sleep(1000);
        }
        Thread.sleep(1000);
        target.interrupt();
      } catch (InterruptedException ie) {
        
      }
    }).start();
    Connection leak = null;
    try {
      leak = CLUSTER.newConnection();
    } catch (ConnectionException ce) {
      // expected
    }
    assertNull(leak);
    Thread maker = lookForConnectionMaker();
    if (maker != null) {
      for (int x=0;x<1000 && maker.isAlive();x++) {
        System.gc();
        Exception printer = new Exception("trying to join:" + x);
        printer.setStackTrace(maker.getStackTrace());
        printer.printStackTrace();
        maker.join(1000);
      }
      assertFalse(maker.isAlive());
    }
  }
  
  private static Thread lookForConnectionMaker() {
    Thread[] list = new Thread[Thread.activeCount()];
    Thread.enumerate(list);
    for (Thread t : list) {
      if (t.getName().startsWith("Connection Maker")) {
        return t;
      }
    }
    return null;
  }  
  
  private static Thread lookForConnectionEstablisher() {
    Thread[] list = new Thread[Thread.activeCount()];
    Thread.enumerate(list);
    for (Thread t : list) {
      if (t != null && t.getName().startsWith("ConnectionEstablisher")) {
        return t;
      }
    }
    return null;
  }
}
