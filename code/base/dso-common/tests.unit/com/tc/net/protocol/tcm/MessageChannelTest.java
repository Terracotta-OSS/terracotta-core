/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.SequenceGenerator;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import gnu.trove.TLongHashSet;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * This is a test case for MessageChannel. XXX: This test could use some work. It's not very coherent and uses sleeps.
 * --Orion 12/19/2005
 */
public class MessageChannelTest extends TCTestCase {
  static final int             ITERATIONS    = 100;
  static final int             WAIT_PERIOD   = 100;
  static final int             WAIT          = ITERATIONS * WAIT_PERIOD;
  static final int             MESSAGE_COUNT = 250;

  TCLogger                     logger        = TCLogging.getLogger(getClass());
  NetworkListener              lsnr;
  CommunicationsManager        clientComms;
  CommunicationsManager        serverComms;
  TCMessageRouter              clientMessageRouter;
  TCMessageRouter              serverMessageRouter;
  ClientMessageChannel         clientChannel;
  MessageSendAndReceiveWatcher clientWatcher;
  MessageSendAndReceiveWatcher serverWatcher;
  SynchronizedRef              error         = new SynchronizedRef(null);
  SequenceGenerator            sq            = new SequenceGenerator();

  private int                  port          = 0;

  // public MessageChannelTest() {
  // disableAllUntil("2006-02-15");
  // }

  protected void setUp(int maxReconnectTries) throws Exception {
    setUp(maxReconnectTries, false);
  }

  protected void setUp(int maxReconnectTries, boolean allowConnectionReplace) throws Exception {
    setUp(maxReconnectTries, allowConnectionReplace, false);
  }

  protected void setUp(int maxReconnectTries, boolean allowConnectionReplace, boolean dumbSink) throws Exception {
    setUp(maxReconnectTries, new PlainNetworkStackHarnessFactory(allowConnectionReplace),
          new PlainNetworkStackHarnessFactory(allowConnectionReplace), dumbSink);
  }

  protected void setUp(int maxReconnectTries, NetworkStackHarnessFactory clientStackHarnessFactory,
                       NetworkStackHarnessFactory serverStackHarnessFactory, boolean dumbServerSink) throws Exception {
    super.setUp();

    clientWatcher = new MessageSendAndReceiveWatcher();
    serverWatcher = new MessageSendAndReceiveWatcher();

    clientMessageRouter = new TCMessageRouterImpl();
    serverMessageRouter = new TCMessageRouterImpl();
    MessageMonitor mm = new NullMessageMonitor();
    clientComms = new CommunicationsManagerImpl("TestCommMgr-client", mm, clientMessageRouter,
                                                clientStackHarnessFactory, new NullConnectionPolicy(),
                                                new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
                                                Collections.EMPTY_MAP);

    serverComms = new CommunicationsManagerImpl("TestCommMgr-server", mm, serverMessageRouter,
                                                serverStackHarnessFactory, new NullConnectionPolicy(),
                                                new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
                                                Collections.EMPTY_MAP);

    initListener(clientWatcher, serverWatcher, dumbServerSink);
    this.clientChannel = createClientMessageChannel(maxReconnectTries);
    this.setUpClientReceiveSink();
  }

  private void initListener(final MessageSendAndReceiveWatcher myClientSenderWatcher,
                            final MessageSendAndReceiveWatcher myServerSenderWatcher, boolean dumbServerSink)
      throws IOException, TCTimeoutException {
    if (lsnr != null) {
      lsnr.stop(WAIT);
    }

    serverComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    ((CommunicationsManagerImpl) serverComms).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE,
                                                                                  new TCMessageSink() {
                                                                                    public void putMessage(TCMessage message)
                                                                                        throws UnsupportedMessageTypeException {
                                                                                      // System.out.println(message);

                                                                                      PingMessage ping = (PingMessage) message;
                                                                                      try {
                                                                                        message.hydrate();
                                                                                      } catch (Exception e) {
                                                                                        setError(e);
                                                                                      }
                                                                                      myClientSenderWatcher
                                                                                          .addMessageReceived(ping);

                                                                                      PingMessage pong = ping
                                                                                          .createResponse();
                                                                                      pong.send();
                                                                                      myServerSenderWatcher
                                                                                          .addMessageSent(pong);
                                                                                    }
                                                                                  });
    if (dumbServerSink) {
      lsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(port), false,
                                        new DefaultConnectionIdFactory(), new WireProtocolMessageSink() {

                                          public void putMessage(WireProtocolMessage message) {
                                            // Thanks for the message.
                                            // But i don't give you back anything
                                            // as i am Dumb.
                                          }
                                        }

      );
    } else {
      lsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(port), false,
                                        new DefaultConnectionIdFactory());
    }

    lsnr.start(new HashSet());
    this.port = lsnr.getBindPort();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    final Throwable lastError = (Throwable) error.get();
    if (lastError != null) { throw new Exception(lastError); }

    if (lsnr != null) lsnr.stop(WAIT);
    if (this.clientChannel != null) this.clientChannel.close();
    if (clientComms != null) clientComms.shutdown();
    if (serverComms != null) serverComms.shutdown();
  }

  public void testAttachments() throws Exception {
    setUp(10);
    clientChannel.open();
    String key = "key";
    MessageChannel channel = createClientMessageChannel(10);
    assertNull(channel.getAttachment(key));
    assertNull(channel.removeAttachment(key));

    Object attachment = new Object();
    Object attachment2 = new Object();
    channel.addAttachment(key, attachment, false);
    assertSame(attachment, channel.getAttachment(key));
    channel.addAttachment(key, attachment, false);
    assertSame(attachment, channel.getAttachment(key));

    channel.addAttachment(key, attachment2, true);
    assertSame(attachment2, channel.getAttachment(key));

    Object removed = channel.removeAttachment(key);
    assertSame(attachment2, removed);

    removed = channel.removeAttachment(key);
    assertNull(removed);
    assertNull(channel.getAttachment(key));
  }

  public void testOpenRaceWithAutoReconnect() throws Exception {
    setUp(-1, false, true);

    Thread t = new Thread() {
      @Override
      public void run() {
        ThreadUtil.reallySleep(WAIT / 2);
        serverComms.getConnectionManager().shutdown();
        System.err.println("closed connections on server side");
      }
    };

    t.start();

    try {
      clientChannel.open();
      fail();
    } catch (TCTimeoutException e) {
      // expected;
      System.err.println("Expected: got timeout exception for first open() : " + e);
    }

    try {
      clientChannel.open();
      fail();
    } catch (ConnectException e) {
      // expected
      System.err.println("Expected: Connection Error: " + e);
    }
    clientChannel = null;
  }

  public void testClientSwithOver() throws Exception {

    clientWatcher = new MessageSendAndReceiveWatcher();
    serverWatcher = new MessageSendAndReceiveWatcher();

    MessageMonitor mm = new NullMessageMonitor();
    clientComms = new CommunicationsManagerImpl("TestCommMgr-client", mm, new PlainNetworkStackHarnessFactory(),
                                                new NullConnectionPolicy(), 0);
    CommunicationsManager serverComms1 = new CommunicationsManagerImpl("TestCommMgr-server-1", mm,
                                                                       new PlainNetworkStackHarnessFactory(),
                                                                       new NullConnectionPolicy(), 0);
    CommunicationsManager serverComms2 = new CommunicationsManagerImpl("TestCommMgr-server-2", mm,
                                                                       new PlainNetworkStackHarnessFactory(),
                                                                       new NullConnectionPolicy(), 0);

    addCommsMappingAndRouting(clientWatcher, serverWatcher, true, serverComms1);
    NetworkListener lsnr1 = getListener(clientWatcher, serverWatcher, true, serverComms1);

    addCommsMappingAndRouting(clientWatcher, serverWatcher, false, serverComms2);
    NetworkListener lsnr2 = getListener(clientWatcher, serverWatcher, false, serverComms2);

    CommunicationsManager clComms = new CommunicationsManagerImpl("TestCommMgr-client", mm,
                                                                  new PlainNetworkStackHarnessFactory(),
                                                                  new NullConnectionPolicy(), 0);

    this.setUpClientReceiveSink();
    this.clientChannel = createClientMessageChannel(clComms, -1, lsnr1.getBindPort(), new ConnectionInfo[] {
        new ConnectionInfo("localhost", lsnr1.getBindPort()), new ConnectionInfo("localhost", lsnr2.getBindPort()) });

    try {
      clientChannel.open();
    } catch (TCTimeoutException e) {
      Assert.eval("This is not suppose to happen", false);
    }
    Assert.eval(clientChannel.isConnected());
  }

  private NetworkListener getListener(final MessageSendAndReceiveWatcher clientWatcher2,
                                      final MessageSendAndReceiveWatcher serverWatcher2, boolean dumbServerSink,
                                      CommunicationsManager serverComms1) throws IOException {

    NetworkListener rv;
    if (dumbServerSink) {
      rv = serverComms1.createListener(new NullSessionManager(), new TCSocketAddress(port), false,
                                       new DefaultConnectionIdFactory(), new WireProtocolMessageSink() {

                                         public void putMessage(WireProtocolMessage message) {
                                           // Thanks for the message.
                                           // But i don't give you back anything
                                           // as i am Dumb.
                                         }
                                       }

      );
    } else {
      rv = serverComms1.createListener(new NullSessionManager(), new TCSocketAddress(port), false,
                                       new DefaultConnectionIdFactory());
    }

    rv.start(new HashSet());
    return rv;
  }

  private void addCommsMappingAndRouting(final MessageSendAndReceiveWatcher clientWatcher2,
                                         final MessageSendAndReceiveWatcher serverWatcher2, boolean dumbServerSink,
                                         CommunicationsManager serverComms1) {
    serverComms1.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    ((CommunicationsManagerImpl) serverComms1).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE,
                                                                                   new TCMessageSink() {
                                                                                     public void putMessage(TCMessage message)
                                                                                         throws UnsupportedMessageTypeException {
                                                                                       // System.out.println(message);

                                                                                       PingMessage ping = (PingMessage) message;
                                                                                       try {
                                                                                         message.hydrate();
                                                                                       } catch (Exception e) {
                                                                                         setError(e);
                                                                                       }
                                                                                       clientWatcher2
                                                                                           .addMessageReceived(ping);

                                                                                       PingMessage pong = ping
                                                                                           .createResponse();
                                                                                       pong.send();
                                                                                       serverWatcher2
                                                                                           .addMessageSent(pong);
                                                                                     }
                                                                                   });
  }

  public void testAutomaticReconnect() throws Exception {
    setUp(10, true);
    assertEquals(0, clientChannel.getConnectCount());
    assertEquals(0, clientChannel.getConnectAttemptCount());
    clientChannel.open();
    assertEquals(1, clientChannel.getConnectCount());
    assertEquals(1, clientChannel.getConnectAttemptCount());

    final int closeCount = new Random().nextInt(MESSAGE_COUNT);

    for (int i = 0; i < MESSAGE_COUNT; i++) {
      if (i == closeCount) {
        waitForArrivalOrFail(clientWatcher, i);
        waitForArrivalOrFail(serverWatcher, i);
        clientComms.getConnectionManager().closeAllConnections(WAIT);
        if (!waitUntilReconnected()) {
          fail("Didn't reconnect");
        }
      }
      createAndSendMessage();
    }
    assertTrue(clientChannel.getConnectAttemptCount() > 1);
    assertTrue(clientChannel.getConnectCount() > 1);

    waitForMessages(MESSAGE_COUNT);
  }

  private void waitForMessages(int count) throws InterruptedException {
    waitForArrivalOrFail(clientWatcher, count);
    waitForArrivalOrFail(serverWatcher, count);

    String msg = "expected: " + count + ", client sent: " + clientWatcher.sent() + ", client received: "
                 + clientWatcher.received() + ", server sent: " + serverWatcher.sent() + ", server received: "
                 + serverWatcher.received();

    assertEquals(msg, count, clientWatcher.sent());
    assertEquals(msg, count, clientWatcher.received());
    assertEquals(msg, count, serverWatcher.sent());
    assertEquals(msg, count, serverWatcher.received());
  }

  public void testManualReconnectAfterFailure() throws Exception {
    setUp(0);

    lsnr.stop(WAIT);
    serverComms.getConnectionManager().closeAllConnections(WAIT);
    clientComms.getConnectionManager().closeAllConnections(WAIT);

    for (int i = 0; i < 10; i++) {
      try {
        clientChannel.open();
        fail("Should have thrown an exception");
      } catch (TCTimeoutException e) {
        // expected
      } catch (UnknownHostException e) {
        fail(e.getMessage());
      } catch (IOException e) {
        // expected
      }

      assertFalse(clientChannel.isConnected());
    }

    initListener(this.clientWatcher, this.serverWatcher, false);
    clientChannel.open();
    assertTrue(clientChannel.isConnected());
  }

  public void testSendAfterDisconnect() throws Exception {
    setUp(0);
    clientChannel.open();

    createAndSendMessage();
    waitForArrivalOrFail(clientWatcher, 1);
    waitForArrivalOrFail(serverWatcher, 1);

    sendMessagesWhileDisconnected(MESSAGE_COUNT, 25);

    // don't explicitly need to do this, but if we wait, it's possible an error will happen on another thread
    ThreadUtil.reallySleep(5000);
  }

  public void testZeroMaxRetriesDoesntAutoreconnect() throws Exception {
    setUp(0);
    assertEquals(0, clientChannel.getConnectAttemptCount());
    assertEquals(0, clientChannel.getConnectCount());

    clientChannel.open();
    assertEquals(1, clientChannel.getConnectAttemptCount());
    assertEquals(1, clientChannel.getConnectCount());
    clientComms.getConnectionManager().closeAllConnections(WAIT);
    ThreadUtil.reallySleep(5000);
    assertEquals(1, clientChannel.getConnectAttemptCount());
    assertEquals(1, clientChannel.getConnectCount());
  }

  public void testNegativeMaxRetriesAlwaysReconnects() throws Exception {
    setUp(-1);

    assertEquals(0, clientChannel.getConnectCount());
    assertEquals(0, clientChannel.getConnectAttemptCount());

    clientChannel.open();

    assertEquals(1, clientChannel.getConnectCount());
    assertEquals(1, clientChannel.getConnectAttemptCount());

    lsnr.stop(WAIT);
    assertEquals(0, serverComms.getAllListeners().length);

    clientComms.getConnectionManager().closeAllConnections(5000);
    int count = clientChannel.getConnectAttemptCount();
    ThreadUtil.reallySleep(WAIT * 4);
    assertTrue(clientChannel.getConnectAttemptCount() + " vs " + count, clientChannel.getConnectAttemptCount() > count);
    assertEquals(1, clientChannel.getConnectCount());
  }

  // public void testSendBeforeOpen() throws Exception {
  // setUp(0);
  // PingMessage ping = createMessage();
  // assertTrue(clientChannel.getStatus().isClosed());
  // try {
  // ping.send();
  // fail("Should have thrown an assertion error");
  // } catch (TCAssertionError e) {
  // // expected
  // }
  // }
  //
  // public void testSendAfterClose() throws Exception {
  // setUp(0);
  // clientChannel.open();
  // assertTrue(clientChannel.getStatus().isOpen());
  //
  // PingMessage ping = createMessage();
  // clientChannel.close();
  // assertTrue(clientChannel.isClosed());
  //
  // try {
  // // send should fail
  // ping.send();
  // fail("should have thrown an exception");
  // } catch (TCAssertionError err) {
  // // expected
  // }
  // }

  public void testGetStatus() throws Exception {
    setUp(0);
    clientChannel.open();
    assertTrue(clientChannel.isOpen());
    clientChannel.close();
    assertTrue(clientChannel.isClosed());
  }

  public void testSend() throws Exception {
    setUp(0);
    clientChannel.open();
    int count = 100;
    List messages = new LinkedList();
    for (int i = 0; i < count; i++) {
      messages.add(createAndSendMessage());
    }
    waitForMessages(count);

  }

  public void testSocketInfo() throws Exception {
    setUp(0);

    assertNull(clientChannel.getRemoteAddress());
    assertNull(clientChannel.getLocalAddress());

    clientChannel.open();
    createAndSendMessage();
    waitForMessages(1);

    TCSocketAddress clientRemote = clientChannel.getRemoteAddress();
    TCSocketAddress clientLocal = clientChannel.getLocalAddress();

    MessageChannelInternal[] serverChannels = lsnr.getChannelManager().getChannels();
    assertEquals(1, serverChannels.length);
    MessageChannelInternal serverChannel = serverChannels[0];

    TCSocketAddress serverRemote = serverChannel.getRemoteAddress();
    TCSocketAddress serverLocal = serverChannel.getLocalAddress();

    assertEquals(clientRemote, serverLocal);
    assertEquals(clientLocal, serverRemote);
  }

  private PingMessage createAndSendMessage() {
    PingMessage ping = createMessage();
    clientWatcher.addMessageSent(ping);
    ping.send();
    return ping;
  }

  private static void waitForArrivalOrFail(MessageSendAndReceiveWatcher watcher, int count) throws InterruptedException {
    int i = 0;
    while (!watcher.allReceived() || (watcher.sent() < count) || (watcher.received() < count)) {
      if (i == ITERATIONS) {
        fail((watcher.sent() - watcher.received()) + " messages of " + watcher.sent()
             + " messages total failed to arrive in " + ITERATIONS + " iterations of " + WAIT_PERIOD + " ms. waiting.");
      }

      Thread.sleep(WAIT_PERIOD);
      i++;
    }
  }

  private ClientMessageChannel createClientMessageChannel(int maxReconnectTries) {
    return createClientMessageChannel(clientComms, maxReconnectTries, lsnr.getBindPort(),
                                      new ConnectionInfo[] { new ConnectionInfo("localhost", lsnr.getBindPort()) });
  }

  private ClientMessageChannel createClientMessageChannel(CommunicationsManager clComms, int maxReconnectTries,
                                                          int lsnrPort, final ConnectionInfo[] connInfo) {
    clComms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    ClientMessageChannel ch = clComms.createClientChannel(new NullSessionManager(), maxReconnectTries,
                                                          TCSocketAddress.LOOPBACK_IP, lsnrPort, WAIT,
                                                          new ConnectionAddressProvider(connInfo));
    return ch;
  }

  private PingMessage createMessage() {
    PingMessage ping = (PingMessage) clientChannel.createMessage(TCMessageType.PING_MESSAGE);
    ping.initialize(sq);
    return ping;
  }

  private void sendMessagesWhileDisconnected(int count, int afterCount) throws InterruptedException {
    Random rnd = new Random();
    final int closeCount = rnd.nextInt(count);
    final boolean serverClose = rnd.nextBoolean();

    Thread thread = null;

    for (int i = 0; i < count; i++) {
      if (i == closeCount) {
        // close down the connection in a seperate thread to increase the timing randomness
        thread = new Thread("Connection closer thread") {
          @Override
          public void run() {
            try {
              if (serverClose) {
                logger.info("Initiating close on the SERVER side...");
                serverComms.getConnectionManager().asynchCloseAllConnections();
              } else {
                logger.info("Initiating close on the CLIENT side...");
                clientComms.getConnectionManager().asynchCloseAllConnections();
              }
            } catch (Throwable t) {
              setError(t);
            }
          }
        };
        Thread.sleep(rnd.nextInt(25) + 10);
        thread.setDaemon(true);
        thread.start();
      }

      createAndSendMessage();
    }

    thread.join(WAIT);
    assertFalse(thread.isAlive());

    // make sure we send messages after the connection has actually closed for good measure
    for (int i = 0; i < afterCount; i++) {
      createAndSendMessage();
    }
  }

  private boolean waitUntilReconnected() {
    final long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < WAIT) {
      if (clientChannel.isConnected()) return true;
      try {
        Thread.sleep(WAIT_PERIOD);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  private void setUpClientReceiveSink() {
    final MessageSendAndReceiveWatcher myServerSenderWatcher = this.serverWatcher;
    ((CommunicationsManagerImpl) clientComms).getMessageRouter().routeMessageType(TCMessageType.PING_MESSAGE,
                                                                                  new TCMessageSink() {
                                                                                    public void putMessage(TCMessage message)
                                                                                        throws UnsupportedMessageTypeException {
                                                                                      try {
                                                                                        PingMessage ping = (PingMessage) message;
                                                                                        ping.hydrate();
                                                                                        // System.out.println("CLIENT RECEIVE: "
                                                                                        // + ping.getSequence());
                                                                                      } catch (Exception e) {
                                                                                        setError(e);
                                                                                      }
                                                                                      PingMessage ping = (PingMessage) message;
                                                                                      myServerSenderWatcher
                                                                                          .addMessageReceived(ping);
                                                                                    }
                                                                                  });
  }

  private void setError(Throwable t) {
    synchronized (System.err) {
      System.err.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S").format(new Date())
                         + ": Exception Thrown in thread [" + Thread.currentThread().getName() + "]");
      t.printStackTrace(System.err);
    }
    error.set(t);
  }

  public static class MessageSendAndReceiveWatcher {

    private final TLongHashSet sentSequences     = new TLongHashSet();
    private final TLongHashSet receivedSequences = new TLongHashSet();

    public synchronized void addMessageSent(PingMessage sent) {
      sentSequences.add(sent.getSequence());
    }

    public synchronized void addMessageReceived(PingMessage received) {
      receivedSequences.add(received.getSequence());
    }

    public int sent() {
      return sentSequences.size();
    }

    public int received() {
      return receivedSequences.size();
    }

    public synchronized boolean allReceived() {
      return receivedSequences.containsAll(sentSequences.toArray());
    }
  }
}
