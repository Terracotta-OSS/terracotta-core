/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.impl.NullSink;
import com.tc.cluster.Cluster;
import com.tc.cluster.DsoClusterImpl;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.net.MockChannel;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandshakeManagerTest extends TCTestCase {
  private static final String               clientVersion = "x.y.z";
  private ClientHandshakeManagerImpl        mgr;
  private TestClientHandshakeMessageFactory chmf;
  private TestClientHandshakeCallback       callback;
  private MockChannel                       channel;

  @Override
  public void setUp() throws Exception {
    chmf = new TestClientHandshakeMessageFactory();
    callback = new TestClientHandshakeCallback();
    channel = new MockChannel();
    newMessage();
  }

  private void newMessage() {
    chmf.message = new TestClientHandshakeMessage();
  }

  private void createHandshakeMgr() {
    mgr = new ClientHandshakeManagerImpl(TCLogging.getLogger(ClientHandshakeManagerImpl.class), channel, chmf,
                                         new NullSink(), new NullSessionManager(), new Cluster(), new DsoClusterImpl(), clientVersion,
                                         Collections.singletonList(callback));
  }

  public void tests() {
    createHandshakeMgr();
    GroupID group = channel.groups[0];
    assertEquals(1, callback.paused.get());
    assertEquals(0, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    mgr.connected(group);

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());

    assertFalse(done.get());

    TestClientHandshakeMessage sentMessage = (TestClientHandshakeMessage) chmf.newMessageQueue.take();
    assertTrue(chmf.newMessageQueue.isEmpty());

    // make sure that the manager called send on the message...
    sentMessage.sendCalls.take();
    assertTrue(sentMessage.sendCalls.isEmpty());

    // make sure RuntimeException is thrown if client/server versions don't match and version checking is enabled
    try {
      mgr.acknowledgeHandshake(group, false, new ClientID(new ChannelID(1)), new ClientID[] {}, clientVersion + "a.b.c");
      if (checkVersionMatchEnabled()) {
        fail();
      }
    } catch (RuntimeException e) {
      if (!checkVersionMatchEnabled()) {
        fail();
      }
    }

    // now ACK for real
    mgr.acknowledgeHandshake(group, false, new ClientID(new ChannelID(1)), new ClientID[] {}, clientVersion);

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(1, callback.unpaused.get());

    while (!done.get()) {
      // Will fail with a timeout
      ThreadUtil.reallySleep(1000);
    }
  }

  public void testMultipleGroups() {
    GroupID g0 = new GroupID(0);
    GroupID g1 = new GroupID(1);
    GroupID g2 = new GroupID(2);
    channel.groups = new GroupID[] { g0, g1, g2 };
    createHandshakeMgr();

    assertEquals(1, callback.paused.get());
    assertEquals(0, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());
    assertEquals(3, callback.disconnected);

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    mgr.connected(g0);

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());
    assertEquals(3, callback.disconnected);

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    // now ACK for real
    mgr.acknowledgeHandshake(g0, false, new ClientID(new ChannelID(1)), new ClientID[] {}, clientVersion);

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(1, callback.unpaused.get());
    assertEquals(2, callback.disconnected);

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    mgr.connected(g1);
    mgr.connected(g2);
    assertEquals(1, callback.paused.get());
    assertEquals(3, callback.initiateHandshake.get());
    assertEquals(1, callback.unpaused.get());
    assertEquals(2, callback.disconnected);

    mgr.acknowledgeHandshake(g1, false, new ClientID(new ChannelID(1)), new ClientID[] {}, clientVersion);
    assertEquals(1, callback.paused.get());
    assertEquals(3, callback.initiateHandshake.get());
    assertEquals(2, callback.unpaused.get());
    assertEquals(1, callback.disconnected);

    mgr.acknowledgeHandshake(g2, false, new ClientID(new ChannelID(1)), new ClientID[] {}, clientVersion);
    assertEquals(1, callback.paused.get());
    assertEquals(3, callback.initiateHandshake.get());
    assertEquals(3, callback.unpaused.get());
    assertEquals(0, callback.disconnected);

    while (!done.get()) {
      // Will fail with a timeout
      ThreadUtil.reallySleep(1000);
    }
  }

  private boolean checkVersionMatchEnabled() {
    return TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L1_CONNECT_VERSION_MATCH_CHECK);
  }

  private static class TestClientHandshakeMessageFactory implements ClientHandshakeMessageFactory {

    public TestClientHandshakeMessage   message;
    public final NoExceptionLinkedQueue newMessageQueue = new NoExceptionLinkedQueue();

    public ClientHandshakeMessage newClientHandshakeMessage(final NodeID remoteNode) {
      newMessageQueue.put(message);
      return message;
    }

  }

  public class TestSequenceProvider implements BatchSequenceProvider {

    long sequence = 1;

    public synchronized void requestBatch(final BatchSequenceReceiver receiver, final int size) {
      receiver.setNextBatch(sequence, sequence + size);
      sequence += size;
    }

  }

  private static final class TestClientHandshakeCallback implements ClientHandshakeCallback {

    AtomicInteger paused            = new AtomicInteger();
    AtomicInteger unpaused          = new AtomicInteger();
    AtomicInteger initiateHandshake = new AtomicInteger();
    int           disconnected;

    public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode, final ClientHandshakeMessage handshakeMessage) {
      initiateHandshake.incrementAndGet();
    }

    public void pause(final NodeID remoteNode, final int disconnectedCount) {
      paused.incrementAndGet();
      this.disconnected = disconnectedCount;
    }

    public void unpause(final NodeID remoteNode, final int disconnectedCount) {
      unpaused.incrementAndGet();
      this.disconnected = disconnectedCount;
    }
  }

}
