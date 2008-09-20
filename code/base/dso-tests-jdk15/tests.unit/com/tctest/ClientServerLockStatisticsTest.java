/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.NullTCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.ClientLockStatisticsManagerImpl;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStatElement;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.management.lock.stats.TCStackTraceElement;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.NullClientLockManagerConfig;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.ClientServerLockStatManagerGlue;
import com.tc.object.lockmanager.impl.StripedClientLockManagerImpl;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessageFactory;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionID;
import com.tc.object.session.TestSessionManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClientServerLockStatisticsTest extends TCTestCase {

  private ClientLockManager               clientLockManager;
  private LockManagerImpl                 serverLockManager;
  private ClientServerLockStatManagerGlue clientServerGlue;
  private TestSessionManager              sessionManager;
  private ClientLockStatManager           clientLockStatManager;
  private L2LockStatsManager              serverLockStatManager;
  private ChannelID                       channelId1 = new ChannelID(1);
  private ClientMessageChannel            channel1;
  private TestSink                        sink;

  public ClientServerLockStatisticsTest() {
    // MNK-444
    // disableAllUntil(new Date(Long.MAX_VALUE));
  }

  protected void setUp() throws Exception {
    super.setUp();

    sink = new TestSink();
    sessionManager = new TestSessionManager();
    clientServerGlue = new ClientServerLockStatManagerGlue(sessionManager, sink);
    clientLockStatManager = new ClientLockStatisticsManagerImpl();
    clientLockManager = new StripedClientLockManagerImpl(new NullTCLogger(), clientServerGlue, sessionManager,
                                                         clientLockStatManager, new NullClientLockManagerConfig());

    DSOChannelManager nullChannelManager = new NullChannelManager();
    serverLockStatManager = new L2LockStatisticsManagerImpl();
    serverLockManager = new LockManagerImpl(nullChannelManager, serverLockStatManager);
    serverLockManager.setLockPolicy(LockManagerImpl.ALTRUISTIC_LOCK_POLICY);

    channel1 = new TestClientMessageChannel(channelId1, sink);
    serverLockStatManager.start(new TestChannelManager(channel1));

    clientLockStatManager.start(new TestClientChannel(channel1), sink);
    clientServerGlue.set(clientLockManager, serverLockManager, clientLockStatManager, serverLockStatManager);
  }

  public void testClientDisconnect() {
    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    DSOChannelManager nullChannelManager = new NullChannelManager();
    serverLockStatManager = new MockL2LockStatManagerImpl(localBarrier);
    serverLockManager = new LockManagerImpl(nullChannelManager, serverLockStatManager);

    serverLockStatManager.start(new TestChannelManager(channel1));
    clientServerGlue.set(clientLockManager, serverLockManager, clientLockStatManager, serverLockStatManager);

    final LockID l1 = new LockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(1);

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        serverLockManager.clearAllLocksFor(new ClientID(channelId1));
        try {
          localBarrier.await();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        } catch (BrokenBarrierException e) {
          throw new AssertionError(e);
        }
      }
    });
    serverLockStatManager.setLockStatisticsConfig(1, 1);
    clientLockManager.lock(l1, tx1, LockLevel.WRITE, null, "lock manually");
    t1.start();
    Collection c = serverLockStatManager.getLockSpecs();
    Assert.assertEquals(1, c.size());
    LockSpec lockSpec = (LockSpec) c.iterator().next();
    Assert.assertEquals(0, lockSpec.children().size());

    clientLockManager.unlock(l1, tx2);
  }

  public void testCollectLockStackTraces() {
    final LockID lockID1 = new LockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(1);

    serverLockStatManager.setLockStatisticsConfig(1, 1);
    clientLockManager.lock(lockID1, tx1, LockLevel.READ, null, "lock manually");
    sleep(1000);

    clientLockManager.lock(lockID1, tx2, LockLevel.READ, null, "lock manually");
    sleep(2000);
    assertStackTraces(lockID1, 2, 1);
    assertStackTraces(lockID1, 2, 1);

    clientLockManager.unlock(lockID1, tx2);
    sleep(2000);
    assertStackTraces(lockID1, 3, 1);
    assertStackTraces(lockID1, 3, 1);

    clientLockManager.unlock(lockID1, tx1);
    sleep(1000);
    assertStackTraces(lockID1, 4, 1);
    assertStackTraces(lockID1, 4, 1);

    serverLockStatManager.setLockStatisticsConfig(2, 1);
    sleep(1000);
    clientLockManager.lock(lockID1, tx2, LockLevel.WRITE, null, "lock manually");
    sleep(1000);
    clientLockManager.lock(lockID1, tx2, LockLevel.WRITE, null, "lock manually");
    sleep(2000);
    assertStackTraces(lockID1, 2, 2);
    assertStackTraces(lockID1, 2, 2);
    clientLockManager.unlock(lockID1, tx2);
    assertStackTraces(lockID1, 3, 2);
    assertStackTraces(lockID1, 3, 2);
    clientLockManager.unlock(lockID1, tx2);
  }

  private void assertStackTraces(LockID lockID, int numOfStackTraces, int depthOfStackTraces) {
    Collection lockSpecs = serverLockStatManager.getLockSpecs();

    Assert.assertEquals(1, lockSpecs.size()); // only one client in this test
    for (Iterator i = lockSpecs.iterator(); i.hasNext();) {
      LockSpec s = (LockSpec) i.next();
      Collection children = s.children();
      Assert.assertTrue(assertStackTracesDepth(children, depthOfStackTraces));
    }
  }

  private boolean assertStackTracesDepth(Collection traces, int expectedDepthOfStackTraces) {
    if (traces.size() == 0 && expectedDepthOfStackTraces == 0) { return true; }
    if (traces.size() == 0 || expectedDepthOfStackTraces == 0) { return false; }

    LockStatElement lse = (LockStatElement) traces.iterator().next();
    return assertStackTracesDepth(lse.children(), expectedDepthOfStackTraces - 1);
  }

  private void sleep(long l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      // NOP
    }
  }

  protected void tearDown() throws Exception {
    clientServerGlue.stop();
    super.tearDown();
  }

  private static class TestChannelManager extends NullChannelManager {
    private MessageChannel channel;

    public TestChannelManager(MessageChannel channel) {
      this.channel = channel;
    }

    public MessageChannel[] getActiveChannels() {
      return new MessageChannel[] { channel };
    }
  }

  private static class TestClientMessageChannel extends MockMessageChannel implements ClientMessageChannel {
    private Sink sink;

    public TestClientMessageChannel(ChannelID channelId, Sink sink) {
      super(channelId);
      super.registerType(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessage.class);
      super.registerType(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
      this.sink = sink;
    }

    public TCMessage createMessage(TCMessageType type) {
      Class theClass = super.getRegisteredMessageClass(type);

      if (theClass == null) throw new ImplementMe();

      try {
        Constructor constructor = theClass.getConstructor(new Class[] { SessionID.class, MessageMonitor.class,
            TCByteBufferOutputStream.class, MessageChannel.class, TCMessageType.class });
        TCMessageImpl message = (TCMessageImpl) constructor.newInstance(new Object[] { SessionID.NULL_ID,
            new NullMessageMonitor(), new TCByteBufferOutputStream(4, 4096, false), this, type });
        // message.seal();
        return message;
      } catch (Exception e) {
        throw new ImplementMe("Failed", e);
      }
    }

    public void send(TCNetworkMessage message) {
      super.send(message);
      sink.add(message);
    }

    public void addClassMapping(TCMessageType type, Class msgClass) {
      throw new ImplementMe();

    }

    public ChannelIDProvider getChannelIDProvider() {
      return null;
    }

    public int getConnectAttemptCount() {
      return 0;
    }

    public int getConnectCount() {
      return 0;
    }

    public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
      throw new ImplementMe();

    }

    public void routeMessageType(TCMessageType type, TCMessageSink sinkArg) {
      throw new ImplementMe();

    }

    public void unrouteMessageType(TCMessageType type) {
      throw new ImplementMe();

    }

  }

  private static class TestClientChannel implements DSOClientMessageChannel {
    private ClientMessageChannel clientMessageChannel;

    public TestClientChannel(ClientMessageChannel clientMessageChannel) {
      this.clientMessageChannel = clientMessageChannel;
    }

    public ClientMessageChannel channel() {
      return clientMessageChannel;
    }

    public void addClassMapping(TCMessageType messageType, Class messageClass) {
      throw new ImplementMe();

    }

    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();

    }

    public void close() {
      throw new ImplementMe();

    }

    public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
      throw new ImplementMe();
    }

    public ChannelIDProvider getChannelIDProvider() {
      throw new ImplementMe();
    }

    public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
      throw new ImplementMe();
    }

    public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
      throw new ImplementMe();
    }

    public JMXMessage getJMXMessage() {
      throw new ImplementMe();
    }

    public LockRequestMessageFactory getLockRequestMessageFactory() {
      throw new ImplementMe();
    }

    public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
      throw new ImplementMe();
    }

    public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
      throw new ImplementMe();
    }

    public RequestRootMessageFactory getRequestRootMessageFactory() {
      throw new ImplementMe();
    }

    public boolean isConnected() {
      throw new ImplementMe();
    }

    public void open() {
      throw new ImplementMe();

    }

    public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
      throw new ImplementMe();

    }

    public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
      throw new ImplementMe();
    }
  }

  private static class MockL2LockStatManagerImpl extends L2LockStatisticsManagerImpl {
    private final CyclicBarrier barrier;

    public MockL2LockStatManagerImpl(CyclicBarrier barrier) {
      super();
      this.barrier = barrier;
    }

    public void recordClientStat(NodeID nodeID, Collection<TCStackTraceElement> stackTraceElements) {
      try {
        barrier.await();
        super.recordClientStat(nodeID, stackTraceElements);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } catch (BrokenBarrierException e) {
        throw new AssertionError(e);
      }
    }
  }

}
