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
import org.terracotta.utilities.test.net.PortManager;

import com.tc.config.GroupConfiguration;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.net.NodeID;
import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.CallableWaiter;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.mock;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;


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
    ServerEnv.setDefaultServer(mock(Server.class));
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

    PortManager portManager = PortManager.getInstance();
    List<PortManager.PortRef> ports = portManager.reservePorts(nodes);
    List<PortManager.PortRef> groupPorts = portManager.reservePorts(nodes);
    try {
      Node[] allNodes = new Node[nodes];
      for (int i = 0; i < nodes; ++i) {
        allNodes[i] = new Node(LOCALHOST, ports.get(i).port(), groupPorts.get(i).port());
      }

      for (int i = 0; i < nodes; ++i) {
        TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), allNodes[i].getHost(),
                                                       allNodes[i].getPort(), allNodes[i].getGroupPort(),
                                                       stages.createStageManager(),
                                                       RandomWeightGenerator.createTestingFactory(2), allNodes);
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
        GroupConfiguration groupConfiguration = TCGroupManagerImplTest.getGroupConfiguration(nodeSet, allNodes[i]);
        groupManagers[i].join(groupConfiguration);
      }
      waitForAllMessageCountsToReach(nodes - 1);

      System.out.println("VERIFIED");
      shutdown();
    } finally {
      ports.forEach(PortManager.PortRef::close);
      groupPorts.forEach(PortManager.PortRef::close);
    }
  }

  private void nodesSetupAndJoined_DEV3101(int nodes) throws Exception {
    System.out.println("*** Testing DEV3101 1");
    Assert.assertEquals(2, nodes);

    int count = getThreadCountByName(ClientConnectionEstablisher.RECONNECT_THREAD_NAME);
    System.out.println("XXX Thread count : " + ClientConnectionEstablisher.RECONNECT_THREAD_NAME + " - " + count);

    listeners = new MyListener[nodes];
    groupManagers = new TCGroupManagerImpl[nodes];
    TCPProxy[] proxy = new TCPProxy[nodes];

    PortManager portManager = PortManager.getInstance();
    List<PortManager.PortRef> ports = portManager.reservePorts(nodes);
    List<PortManager.PortRef> groupPorts = portManager.reservePorts(nodes);
    List<PortManager.PortRef> proxyPorts = portManager.reservePorts(nodes);
    try {
      Node[] allNodes = new Node[nodes];
      Node[] proxiedAllNodes = new Node[nodes];
      for (int i = 0; i < nodes; ++i) {
        int port = ports.get(i).port();
        int groupPort = groupPorts.get(i).port();
        int proxyPort = proxyPorts.get(i).port();
        allNodes[i] = new Node(LOCALHOST, port, groupPort);
        proxy[i] = new TCPProxy(proxyPort, InetAddress.getByName(LOCALHOST), groupPort, 0, false, null);
        proxy[i].start();
        proxiedAllNodes[i] = new Node(LOCALHOST, port, proxyPort);
      }

      for (int i = 0; i < nodes; ++i) {
        TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), allNodes[i].getHost(),
                                                       allNodes[i].getPort(), allNodes[i].getGroupPort(),
                                                       stages.createStageManager(), RandomWeightGenerator.createTestingFactory(2), allNodes);
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

      for (int i = 0; i < nodes; ++i) {
        GroupConfiguration groupConfiguration = TCGroupManagerImplTest.getGroupConfiguration(nodeSet, allNodes[i]);
        groupManagers[i].join(groupConfiguration);
      }

      waitForAllMessageCountsToReach(nodes - 1);

      System.out.println("XXX 1st verification done");

      for (int i = 0; i < nodes; ++i) {
        proxy[i].stop();
      }

      System.out.println("XXX Node 0 Zapped Node 1");
      groupManagers[0].addZappedNode(groupManagers[1].getLocalNodeID());

      System.out.println("XXX proxy stopped");
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

      ensureThreadAbsent(ClientConnectionEstablisher.RECONNECT_THREAD_NAME, count);
      shutdown();
    } finally {
      Arrays.stream(proxy).filter(Objects::nonNull).forEach(TCPProxy::stop);
      proxyPorts.forEach(PortManager.PortRef::close);
      ports.forEach(PortManager.PortRef::close);
      groupPorts.forEach(PortManager.PortRef::close);
    }
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
        for (TCGroupMember member : groupMgr[i].getMembers()) {
          member.close();
        }
        groupMgr[i].stop(1000);
      } catch (Throwable ex) {
        System.out.println("*** Failed to stop Server[" + i + "] " + groupMgr[i] + " " + ex);
      }
    }
    this.stages.shutdown();
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
