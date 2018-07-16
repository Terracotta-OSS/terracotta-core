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
package com.tc.net.groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.StageManager;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.CallableWaiter;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;


public class TCGroupManagerNodeJoinedTest extends TCTestCase {

  private final static String   LOCALHOST = "localhost";
  private static final Logger logger    = LoggerFactory.getLogger(TCGroupManagerNodeJoinedTest.class);
  private TCThreadGroup         threadGroup;
  private TCGroupManagerImpl[]  groupManagers;
  private MyListener[]          listeners;
  private TestThrowableHandler throwableHandler;
  private MockStageManagerFactory stages;

  @Override
  public void setUp() {
    throwableHandler = new TestThrowableHandler(logger);
    threadGroup = new TCThreadGroup(throwableHandler, "TCGroupManagerNodeJoinedTest");
    stages = new MockStageManagerFactory(logger, new ThreadGroup(threadGroup, "stage-managers"));
  }

  @Override
  protected void tcTestCaseTearDown(Throwable testException) throws Throwable {
    super.tcTestCaseTearDown(testException);
    throwableHandler.throwIfNecessary();
    stages.shutdown();
  }

  public void testNodejoinedTwoServers() throws Exception {
    nodesSetupAndJoined(2);
  }

  public void testNoConnectionThreadLeak() throws Exception {
    nodesSetupAndJoined_DEV3101(2);
  }

  public void testNoConnectionThreadLeakOnL2Reconnect() throws Exception {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");
    nodesSetupAndJoined_DEV3101(2);
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "false");
  }

  // Test for DEV-4870
  public void testNodeJoinAfterCloseMember() throws Exception {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");
    nodesSetupAndJoinedAfterCloseMember(2);
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "false");
  }

  public void testNodejoinedThreeServers() throws Exception {
    nodesSetupAndJoined(3);
  }

  public void testNodejoinedSixServers() throws Exception {
    nodesSetupAndJoined(6);
  }

  // -----------------------------------------------------------------------

  private void nodesSetupAndJoined(int nodes) throws Exception {
    System.out.println("*** Testing nodejoined for " + nodes + " servers");

    listeners = new MyListener[nodes];
    groupManagers = new TCGroupManagerImpl[nodes];
    Node[] allNodes = new Node[nodes];
    PortChooser pc = new PortChooser();
    for (int i = 0; i < nodes; ++i) {
      int port = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, port, port + 1);
    }

    for (int i = 0; i < nodes; ++i) {
      TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), allNodes[i].getHost(),
                                                     allNodes[i].getPort(), allNodes[i].getGroupPort(), stages.createStageManager(), RandomWeightGenerator.createTestingFactory(2));
      gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm, allNodes[i]));

      groupManagers[i] = gm;
      MyGroupEventListener gel = new MyGroupEventListener(gm);
      listeners[i] = new MyListener();
      gm.registerForMessages(TestMessage.class, listeners[i]);
      gm.registerForGroupEvents(gel);

    }

    // joining
    System.out.println("*** Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      Set<Node> nodeSet = new HashSet<>();
      Collections.addAll(nodeSet, allNodes);
      NodesStore nodeStore = new NodesStoreImpl(nodeSet);
      groupManagers[i].join(allNodes[i], nodeStore);
    }
    waitForAllMessageCountsToReach(nodes - 1);

    System.out.println("VERIFIED");
    shutdown();
  }

  public void nodesSetupAndJoinedAfterCloseMember(int nodes) throws Exception {
    System.out.println("XXX Testing DEV-4870 : There is a world after doing closeMember()");
    Assert.assertEquals(2, nodes);

    int count = getThreadCountByName(ClientConnectionEstablisher.RECONNECT_THREAD_NAME);
    System.out.println("XXX Thread count : " + ClientConnectionEstablisher.RECONNECT_THREAD_NAME + " - " + count);

    listeners = new MyListener[nodes];
    groupManagers = new TCGroupManagerImpl[nodes];
    Node[] allNodes = new Node[nodes];
    Node[] proxiedAllNodes = new Node[nodes];
    TCPProxy[] proxy = new TCPProxy[nodes];
    PortChooser pc = new PortChooser();
    for (int i = 0; i < nodes; ++i) {
      int port = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, port, port + 1);

      int proxyPort = pc.chooseRandomPort();
      proxy[i] = new TCPProxy(proxyPort, InetAddress.getByName(LOCALHOST), port + 1, 0, false, null);
      proxy[i].start();
      proxiedAllNodes[i] = new Node(LOCALHOST, port, proxyPort);
    }

    proxy[1].stop();

    for (int i = 0; i < nodes; ++i) {
      StageManager stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
      TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), allNodes[i].getHost(),
                                                     allNodes[i].getPort(), allNodes[i].getGroupPort(), stages.createStageManager(), RandomWeightGenerator.createTestingFactory(2));
      gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm, allNodes[i]));

      groupManagers[i] = gm;
      gm.setZapNodeRequestProcessor(new TCGroupManagerImplTest.MockZapNodeRequestProcessor());
      MyGroupEventListener gel = new MyGroupEventListener(gm);
      listeners[i] = new MyListener();
      gm.registerForMessages(TestMessage.class, listeners[i]);
      gm.registerForGroupEvents(gel);

    }

    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, proxiedAllNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);

    // joining
    System.err.println("XXX Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      groupManagers[i].join(allNodes[i], nodeStore);
    }

    waitForAllMessageCountsToReach(nodes - 1);

    ThreadUtil.reallySleep(5000);

    System.err.println("XXX 1st verification done.");
    System.err.println("XXX Node 0: " + allNodes[0]);
    System.err.println("XXX Node 1: " + allNodes[1]);

    groupManagers[0].closeMember((ServerID) groupManagers[1].getLocalNodeID());
    System.out.println("XXX member close done");

    proxy[0].stop();
    proxy[1].start();

    waitForAllMessageCountsToReach(nodes);

    shutdown();
  }

  public void nodesSetupAndJoined_DEV3101(int nodes) throws Exception {
    System.out.println("*** Testing DEV3101 1");
    Assert.assertEquals(2, nodes);

    int count = getThreadCountByName(ClientConnectionEstablisher.RECONNECT_THREAD_NAME);
    System.out.println("XXX Thread count : " + ClientConnectionEstablisher.RECONNECT_THREAD_NAME + " - " + count);

    listeners = new MyListener[nodes];
    groupManagers = new TCGroupManagerImpl[nodes];
    Node[] allNodes = new Node[nodes];
    Node[] proxiedAllNodes = new Node[nodes];
    TCPProxy[] proxy = new TCPProxy[nodes];
    PortChooser pc = new PortChooser();
    for (int i = 0; i < nodes; ++i) {
      int port = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, port, port + 1);

      int proxyPort = pc.chooseRandomPort();
      proxy[i] = new TCPProxy(proxyPort, InetAddress.getByName(LOCALHOST), port + 1, 0, false, null);
      proxy[i].start();
      proxiedAllNodes[i] = new Node(LOCALHOST, port, proxyPort);
    }

    for (int i = 0; i < nodes; ++i) {
      TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), allNodes[i].getHost(),
                                                     allNodes[i].getPort(), allNodes[i].getGroupPort(), stages.createStageManager(), RandomWeightGenerator.createTestingFactory(2));
      gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm, allNodes[i]));

      groupManagers[i] = gm;
      gm.setZapNodeRequestProcessor(new TCGroupManagerImplTest.MockZapNodeRequestProcessor());
      MyGroupEventListener gel = new MyGroupEventListener(gm);
      listeners[i] = new MyListener();
      gm.registerForMessages(TestMessage.class, listeners[i]);
      gm.registerForGroupEvents(gel);

    }

    // joining
    System.out.println("*** Start Joining...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, proxiedAllNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    for (int i = 0; i < nodes; ++i) {
      groupManagers[i].join(allNodes[i], nodeStore);
    }

    waitForAllMessageCountsToReach(nodes - 1);

    System.out.println("XXX 1st verification done");

    for (int i = 0; i < nodes; ++i) {
      proxy[i].stop();
    }

    System.out.println("XXX Node 0 Zapped Node 1");
    groupManagers[0].addZappedNode(groupManagers[1].getLocalNodeID());

    System.out.println("XXX proxy stopped");
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED)) {
      ThreadUtil.reallySleep(TCPropertiesImpl.getProperties()
          .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT));
    }
    ThreadUtil.reallySleep(5000);

    for (int i = 0; i < nodes; ++i) {
      proxy[i].start();
    }
    System.out.println("XXX proxy resumed. Grp Mgrs discovery started for 20 seconds");

    // let the restores/reconnects along with the zapping problem happen for 20 seconds
    ThreadUtil.reallySleep(20000);

    System.out.println("XXX STOPPING Grp Mgrs discovery");
    for (int i = 0; i < nodes; ++i) {
      groupManagers[i].getDiscover().stop(Integer.MAX_VALUE);
    }

    System.out.println("XXX Waiting for all restore connection close");
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED)) {
      ThreadUtil.reallySleep(ClientMessageTransport.TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT);
    }

    ensureThreadAbsent(ClientConnectionEstablisher.RECONNECT_THREAD_NAME, count);
    shutdown();
  }

  private void ensureThreadAbsent(String absentThreadName, int allowedLimit) {
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
    int count = 0;
    for (Thread t : allThreads) {
      if (t.isAlive() && t.getName().contains(absentThreadName)) {
//  one more chance to wait for death
        try {
//  mutliple tests can be running in the same JVM so this is kind of bogus.
//  just wait until they all finish.  If there is a problem in one of the 
//  tests, things will just hang.
          t.join(5000);
        } catch (InterruptedException ie) {
          throw Assert.failure("trouble joining", ie);
        }
        if (t.isAlive()) {
//  still alive, track it
          System.out.println("XXX " + t);
          for (StackTraceElement ste : t.getStackTrace()) {
            System.out.println("   " + ste);
          }
          count++;
        }
      }
    }
    System.out.println("XXX count = " + count + "; allowedlimit = " + allowedLimit);
    Assert.eval(count <= allowedLimit);
  }

  private int getThreadCountByName(String threadName) {
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
    int count = 0;
    for (Thread t : allThreads) {
      if (t.getName().contains(threadName)) {
        count++;
      }
    }
    return count;
  }

  private void shutdown() {
    shutdown(groupManagers, 0, groupManagers.length);
    Thread[] allThreads = ThreadDumpUtil.getAllThreads();
//  clean up threads from previous run
    for (Thread t : allThreads) {
      if (t.isAlive() && t.getName().contains(ClientConnectionEstablisher.RECONNECT_THREAD_NAME)) {
        try {
          t.interrupt(); // break any waits in the select loop
          t.join();
        } catch (InterruptedException ie) {
          throw new AssertionError("trouble shutting down test", ie);
        }
      }
    }
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr, int start, int end) {
    for (int i = start; i < end; ++i) {
      try {
        groupMgr[i].stop(1000);
      } catch (Exception ex) {
        System.out.println("*** Failed to stop Server[" + i + "] " + groupMgr[i] + " " + ex);
      }
    }
    System.out.println("*** shutdown done");
  }

  private static class MyGroupEventListener implements GroupEventsListener {

    private final GroupManager groupManager;
    private final NodeID       gmNodeID;

    public MyGroupEventListener(GroupManager groupManager) {
      this.groupManager = groupManager;
      this.gmNodeID = groupManager.getLocalNodeID();
    }

    @Override
    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeJoined -> " + nodeID);
      try {
        groupManager.sendTo(nodeID, new TestMessage("Hello " + nodeID));
      } catch (GroupException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeLeft -> " + nodeID);
    }
  }

  private void waitForAllMessageCountsToReach(int count) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        for (MyListener listener : listeners) {
          if (listener.size() < count) {
            return false;
          } else if (listener.size() > count) {
            throw new AssertionError("Exceeded the expected count. Expected " + count + " got " + listener.size());
          }
        }
        return true;
      }
    });
  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue<GroupMessage> queue = new NoExceptionLinkedQueue<>();
    private int            count = 0;

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
      ++count;
    }

    public int size() {
      return count;
    }

  }

  private static final class TestMessage extends AbstractGroupMessage {

    // to make serialization sane
    public TestMessage() {
      super(0);
    }

    public TestMessage(String message) {
      super(0);
      this.msg = message;
    }

    @Override
    protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
      msg = in.readString();
    }

    @Override
    protected void basicSerializeTo(TCByteBufferOutput out) {
      out.writeString(msg);
    }

    String msg;

    @Override
    public int hashCode() {
      return msg.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    @Override
    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }

}
