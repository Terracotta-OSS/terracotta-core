/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.net.MockChannel;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandshakeManagerTest extends TCTestCase {
  private static final String               clientVersion = "x.y.z";
  private TestClientHandshakeManagerImpl    mgr;
  private TestClientHandshakeMessageFactory chmf;
  private TestClientHandshakeCallback       callback;
  private TestSessionManager                sessMgr;
  private MockChannel                       channel;

  // extend ClientHandshakeManagerImpl to throw RuntimeException instead of system.exit(-1) when version mismatch
  private class TestClientHandshakeManagerImpl extends ClientHandshakeManagerImpl {

    public TestClientHandshakeManagerImpl(final TCLogger logger, final DSOClientMessageChannel channel,
                                          final ClientHandshakeMessageFactory chmf, final Sink pauseSink,
                                          final SessionManager sessionManager, final Sink nullSink,
                                          final String clientVersion,
                                          final Collection<ClientHandshakeCallback> callbacks) {
      super(logger, channel, chmf, pauseSink, sessionManager, nullSink, clientVersion, callbacks);
    }

    @Override
    protected void mismatchExitWay(String msg) {
      throw new RuntimeException(msg);
    }
  }

  @Override
  public void setUp() throws Exception {
    this.chmf = new TestClientHandshakeMessageFactory();
    this.callback = new TestClientHandshakeCallback();
    this.channel = new MockChannel();
    newMessage();
  }

  private void newMessage() {
    this.chmf.message = new TestClientHandshakeMessage();
  }

  private void createHandshakeMgr() {
    List<ClientHandshakeCallback> callbacks = new ArrayList<ClientHandshakeCallback>();
    callbacks.add(this.callback);
    this.sessMgr = new TestSessionManager();
    this.mgr = new TestClientHandshakeManagerImpl(TCLogging.getLogger(ClientHandshakeManagerImpl.class), this.channel,
                                                  this.chmf, new NullSink(), this.sessMgr, new NullSink(),
                                                  clientVersion, callbacks);
  }

  public void tests() {
    createHandshakeMgr();
    GroupID group = this.channel.groups[0];
    assertEquals(1, this.callback.paused.get());
    assertEquals(0, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        ClientHandshakeManagerTest.this.mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    this.mgr.connected(group);

    assertEquals(1, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());

    assertFalse(done.get());

    TestClientHandshakeMessage sentMessage = (TestClientHandshakeMessage) this.chmf.newMessageQueue.take();
    assertTrue(this.chmf.newMessageQueue.isEmpty());

    // make sure that the manager called send on the message...
    sentMessage.sendCalls.take();
    assertTrue(sentMessage.sendCalls.isEmpty());

    // make sure RuntimeException is thrown if client/server versions don't match and version checking is enabled
    try {
      this.mgr.acknowledgeHandshake(group, false, new ClientID(1), new ClientID[] {}, clientVersion + "a.b.c");
      if (checkVersionMatchEnabled()) {
        fail();
      }
    } catch (RuntimeException e) {
      if (!checkVersionMatchEnabled()) {
        fail();
      }
    }

    // now ACK for real
    this.mgr.acknowledgeHandshake(group, false, new ClientID(1), new ClientID[] {}, clientVersion);

    assertEquals(1, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(1, this.callback.unpaused.get());

    while (!done.get()) {
      // Will fail with a timeout
      ThreadUtil.reallySleep(1000);
    }
  }

  public void testCrashAfterConnectedEvent() {
    createHandshakeMgr();
    GroupID group = this.channel.groups[0];
    assertEquals(1, this.callback.paused.get());
    assertEquals(0, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        ClientHandshakeManagerTest.this.mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    this.mgr.connected(group);

    assertEquals(1, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());

    assertFalse(done.get());

    this.mgr.disconnected(group);

    assertEquals(2, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());
  }

  public void testNewSessionForStartingState() {
    createHandshakeMgr();
    GroupID group = this.channel.groups[0];

    assertEquals(-1L, sessMgr.sessionID.toLong());

    this.mgr.connected(group);

    assertEquals(-1L, sessMgr.sessionID.toLong());

    this.mgr.disconnected(group);

    assertEquals(0L, sessMgr.sessionID.toLong());
  }

  public void testMultipleGroups() {
    GroupID g0 = new GroupID(0);
    GroupID g1 = new GroupID(1);
    GroupID g2 = new GroupID(2);
    this.channel.groups = new GroupID[] { g0, g1, g2 };
    createHandshakeMgr();

    assertEquals(1, this.callback.paused.get());
    assertEquals(0, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());
    assertEquals(3, this.callback.disconnected);

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        ClientHandshakeManagerTest.this.mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    this.mgr.connected(g0);

    assertEquals(1, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(0, this.callback.unpaused.get());
    assertEquals(3, this.callback.disconnected);

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    // now ACK for real
    this.mgr.acknowledgeHandshake(g0, false, new ClientID(1), new ClientID[] {}, clientVersion);

    assertEquals(1, this.callback.paused.get());
    assertEquals(1, this.callback.initiateHandshake.get());
    assertEquals(1, this.callback.unpaused.get());
    assertEquals(2, this.callback.disconnected);

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    this.mgr.connected(g1);
    this.mgr.connected(g2);
    assertEquals(1, this.callback.paused.get());
    assertEquals(3, this.callback.initiateHandshake.get());
    assertEquals(1, this.callback.unpaused.get());
    assertEquals(2, this.callback.disconnected);

    this.mgr.acknowledgeHandshake(g1, false, new ClientID(1), new ClientID[] {}, clientVersion);
    assertEquals(1, this.callback.paused.get());
    assertEquals(3, this.callback.initiateHandshake.get());
    assertEquals(2, this.callback.unpaused.get());
    assertEquals(1, this.callback.disconnected);

    this.mgr.acknowledgeHandshake(g2, false, new ClientID(1), new ClientID[] {}, clientVersion);
    assertEquals(1, this.callback.paused.get());
    assertEquals(3, this.callback.initiateHandshake.get());
    assertEquals(3, this.callback.unpaused.get());
    assertEquals(0, this.callback.disconnected);

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

    public ClientHandshakeMessage newClientHandshakeMessage(NodeID remoteNode, String clVersion,
                                                            boolean isEnterpriseClient) {
      this.newMessageQueue.put(this.message);
      return this.message;
    }

  }

  public class TestSequenceProvider implements BatchSequenceProvider {

    long sequence = 1;

    public synchronized void requestBatch(final BatchSequenceReceiver receiver, final int size) {
      receiver.setNextBatch(this.sequence, this.sequence + size);
      this.sequence += size;
    }

  }

  private static final class TestClientHandshakeCallback implements ClientHandshakeCallback {

    AtomicInteger paused            = new AtomicInteger();
    AtomicInteger unpaused          = new AtomicInteger();
    AtomicInteger initiateHandshake = new AtomicInteger();
    int           disconnected;

    public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                    final ClientHandshakeMessage handshakeMessage) {
      this.initiateHandshake.incrementAndGet();
    }

    public void pause(final NodeID remoteNode, final int disconnectedCount) {
      this.paused.incrementAndGet();
      this.disconnected = disconnectedCount;
    }

    public void unpause(final NodeID remoteNode, final int disconnectedCount) {
      this.unpaused.incrementAndGet();
      this.disconnected = disconnectedCount;
    }

    public void shutdown() {
      // NOP
    }
  }

  private class TestSessionManager implements SessionManager, SessionProvider {
    private SessionID           sessionID     = SessionID.NULL_ID;
    private SessionID           nextSessionID = SessionID.NULL_ID;
    private final AtomicInteger counter       = new AtomicInteger(-1);

    public SessionID getSessionID(NodeID nid) {
      return sessionID;
    }

    public SessionID nextSessionID(NodeID nid) {
      if (nextSessionID == SessionID.NULL_ID) {
        nextSessionID = new SessionID(counter.incrementAndGet());
      }
      return (nextSessionID);
    }

    public void newSession(NodeID nid) {
      if (nextSessionID != SessionID.NULL_ID) {
        sessionID = nextSessionID;
        nextSessionID = SessionID.NULL_ID;
      } else {
        sessionID = new SessionID(counter.incrementAndGet());
      }
    }

    public boolean isCurrentSession(NodeID nid, SessionID sessID) {
      return sessID.equals(sessionID);
    }

    public void initProvider(NodeID nid) {
      return;
    }

    public void resetSessionProvider() {
      throw new ImplementMe();
    }

  }

}
