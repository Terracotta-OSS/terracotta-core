/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.groups.GroupID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceGenerator;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import gnu.trove.TLongHashSet;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ClientGroupMessageChannelTest extends TCTestCase {
  static final int               L2_COUNT        = 5;
  static final int               ITERATIONS      = 100;
  static final int               WAIT_PERIOD     = 100;
  static final int               WAIT            = ITERATIONS * WAIT_PERIOD;
  static final int               MESSAGE_COUNT   = 250;

  TCLogger                       logger          = TCLogging.getLogger(getClass());
  CommunicationsManager          clientComms;
  CommunicationsManager[]        serverComms     = new CommunicationsManager[L2_COUNT];
  NetworkListener[]              lsnr            = new NetworkListener[L2_COUNT];
  GroupID[]                      groupIDs;
  ClientGroupMessageChannel      groupChannel;
  SequenceGenerator              sequence;
  MessageSendAndReceiveWatcher[] clientWatcheres = new MessageSendAndReceiveWatcher[L2_COUNT];
  MessageSendAndReceiveWatcher[] serverWatcheres = new MessageSendAndReceiveWatcher[L2_COUNT];
  SynchronizedRef                error           = new SynchronizedRef(null);
  SequenceGenerator              sq              = new SequenceGenerator();

  private int[]                  ports           = new int[L2_COUNT];

  public ClientGroupMessageChannelTest() {
    // disableAllUntil("2007-10-15");
  }

  protected void setUp(int maxReconnectTries) throws Exception {
    setUp(maxReconnectTries, new PlainNetworkStackHarnessFactory(), new PlainNetworkStackHarnessFactory((maxReconnectTries > 0)));
  }

  protected void setUp(int maxReconnectTries, NetworkStackHarnessFactory clientStackHarnessFactory,
                       NetworkStackHarnessFactory serverStackHarnessFactory) throws Exception {
    super.setUp();

    this.sequence = new SequenceGenerator();

    MessageMonitor mm = new NullMessageMonitor();
    clientComms = new CommunicationsManagerImpl(mm, clientStackHarnessFactory, new NullConnectionPolicy());
    for (int i = 0; i < L2_COUNT; ++i) {
      clientWatcheres[i] = new MessageSendAndReceiveWatcher();
      serverWatcheres[i] = new MessageSendAndReceiveWatcher();

      serverComms[i] = new CommunicationsManagerImpl(mm, serverStackHarnessFactory, new NullConnectionPolicy());
      lsnr[i] = initListener(i, serverComms[i], 0, clientWatcheres[i], serverWatcheres[i]);
      ports[i] = lsnr[i].getBindPort();
    }
    groupChannel = createClientMessageChannel(maxReconnectTries);
    groupIDs = this.groupChannel.getGroupIDs();
    for (int i = 0; i < L2_COUNT; ++i) {
      setUpClientReceiveSink(i, groupChannel.getChannel(groupIDs[i]), serverWatcheres[i]);
    }
  }

  private NetworkListener initListener(int index, final CommunicationsManager serverComm, int port,
                                       final MessageSendAndReceiveWatcher myClientSenderWatcher,
                                       final MessageSendAndReceiveWatcher myServerSenderWatcher) throws IOException {
    NetworkListener l2lsnr;

    l2lsnr = serverComm.createListener(new NullSessionManager(), new TCSocketAddress(port), false,
                                       new DefaultConnectionIdFactory());
    l2lsnr.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    l2lsnr.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {

        PingMessage ping = (PingMessage) message;

        try {
          message.hydrate();
        } catch (Exception e) {
          setError(e);
        }

        // System.out.println("Server-"+ndx+" Received:"+ping.getSequence());

        myClientSenderWatcher.addMessageReceived(ping);

        PingMessage pong = ping.createResponse();
        pong.send();
        myServerSenderWatcher.addMessageSent(pong);
      }
    });
    l2lsnr.start(new HashSet());
    return (l2lsnr);
  }

  private void stopServers() throws Exception {
    for (int i = 0; i < L2_COUNT; ++i) {
      NetworkListener l2lsnr = lsnr[i];
      l2lsnr.stop(WAIT);
      CommunicationsManager serverComm = serverComms[i];
      serverComm.getConnectionManager().closeAllConnections(WAIT);
    }
  }

  private void shutServers() throws Exception {
    for (int i = 0; i < L2_COUNT; ++i) {
      NetworkListener l2lsnr = lsnr[i];
      if (l2lsnr != null) l2lsnr.stop(WAIT);
      CommunicationsManager serverComm = serverComms[i];
      if (serverComm != null) serverComm.shutdown();
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    final Throwable lastError = (Throwable) error.get();
    if (lastError != null) { throw new Exception(lastError); }

    if (this.groupChannel != null) this.groupChannel.close();
    if (clientComms != null) clientComms.shutdown();
    shutServers();
  }

  public void testAttachments() throws Exception {
    setUp(10);
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

  public void channelAutomaticReconnect(int ch) throws Exception {
    final int closeCount = new Random().nextInt(MESSAGE_COUNT);

    for (int i = 0; i < MESSAGE_COUNT; i++) {
      if (i == closeCount) {
        waitForArrivalOrFail(clientWatcheres[ch], i);
        waitForArrivalOrFail(serverWatcheres[ch], i);
        clientComms.getConnectionManager().closeAllConnections(WAIT);
        if (!waitUntilReconnected()) {
          fail("Didn't reconnect");
        }
      }
      createAndSendMessage(ch);
    }
    assertTrue(groupChannel.getConnectAttemptCount() > 1);
    assertTrue(groupChannel.getConnectCount() > 1);

    waitForMessages(ch, MESSAGE_COUNT);
  }

  public void testAutomaticReconnect() throws Exception {
    setUp(10);
    assertEquals(0, groupChannel.getConnectCount());
    assertEquals(0, groupChannel.getConnectAttemptCount());
    groupChannel.open();
    assertEquals(L2_COUNT, groupChannel.getConnectCount());
    assertEquals(L2_COUNT, groupChannel.getConnectAttemptCount());

    for (int i = 0; i < L2_COUNT; ++i) {
      logger.info("testAutomaticReconnect doing channel " + i);
      channelAutomaticReconnect(i);
    }
    
    groupChannel.close();
  }

  private void waitForMessages(int ch, int count) throws InterruptedException {
    waitForMessages(count, clientWatcheres[ch], serverWatcheres[ch]);
  }

  private void waitForMessages(int count, MessageSendAndReceiveWatcher clientWatcher,
                               MessageSendAndReceiveWatcher serverWatcher) throws InterruptedException {
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

    stopServers();
    clientComms.getConnectionManager().closeAllConnections(WAIT);

    for (int i = 0; i < 10; i++) {
      try {
        groupChannel.open();
        fail("Should have thrown an exception");
      } catch (TCTimeoutException e) {
        // expected
      } catch (UnknownHostException e) {
        fail(e.getMessage());
      } catch (IOException e) {
        // expected
      }

      assertFalse(groupChannel.isConnected());
    }

    for (int i = 0; i < L2_COUNT; ++i) {
      lsnr[i] = initListener(i, serverComms[i], ports[i], clientWatcheres[i], serverWatcheres[i]);
    }
    groupChannel.open();
    assertTrue(groupChannel.isConnected());
    groupChannel.close();
  }

  public void testSendAfterDisconnect() throws Exception {
    setUp(0);
    groupChannel.open();

    createAndSendMessage();
    waitForArrivalOrFail(clientWatcheres[0], 1);
    waitForArrivalOrFail(serverWatcheres[0], 1);

    sendMessagesWhileDisconnected(MESSAGE_COUNT, 25);

    // don't explicitly need to do this, but if we wait, it's possible an error will happen on another thread
    ThreadUtil.reallySleep(5000);
    
    groupChannel.close();
  }

  public void testZeroMaxRetriesDoesntAutoreconnect() throws Exception {
    setUp(0);
    assertEquals(0, groupChannel.getConnectAttemptCount());
    assertEquals(0, groupChannel.getConnectCount());

    groupChannel.open();
    assertEquals(L2_COUNT, groupChannel.getConnectAttemptCount());
    assertEquals(L2_COUNT, groupChannel.getConnectCount());
    clientComms.getConnectionManager().closeAllConnections(WAIT);
    ThreadUtil.reallySleep(5000);
    assertEquals(L2_COUNT, groupChannel.getConnectAttemptCount());
    assertEquals(L2_COUNT, groupChannel.getConnectCount());
  }

  public void testNegativeMaxRetriesAlwaysReconnects() throws Exception {
    setUp(-1);

    assertEquals(0, groupChannel.getConnectCount());
    assertEquals(0, groupChannel.getConnectAttemptCount());

    groupChannel.open();

    assertEquals(L2_COUNT, groupChannel.getConnectCount());
    assertEquals(L2_COUNT, groupChannel.getConnectAttemptCount());

    for (int i = 0; i < L2_COUNT; ++i) {
      lsnr[i].stop(WAIT);
      assertEquals(0, serverComms[i].getAllListeners().length);
    }

    clientComms.getConnectionManager().closeAllConnections(5000);
    int count = groupChannel.getConnectAttemptCount();
    ThreadUtil.reallySleep(WAIT * 4);
    assertTrue(groupChannel.getConnectAttemptCount() + " vs " + count, groupChannel.getConnectAttemptCount() > count);
    assertEquals(L2_COUNT, groupChannel.getConnectCount());
    
    groupChannel.close();
  }

  public void testGetStatus() throws Exception {
    setUp(0);
    groupChannel.open();
    assertTrue(groupChannel.isOpen());
    groupChannel.close();
    assertTrue(groupChannel.isClosed());
  }

  private void channelSend(int ch) throws Exception {
    int count = 100;
    List messages = new LinkedList();
    for (int i = 0; i < count; i++) {
      messages.add(createAndSendMessage(ch));
    }
    waitForMessages(ch, count);
  }

  public void testSend() throws Exception {
    setUp(0);
    groupChannel.open();

    for (int i = 0; i < L2_COUNT; ++i) {
      channelSend(i);
    }
    
    groupChannel.close();
  }

  public void testSendMultiplex() throws Exception {
    setUp(0);
    groupChannel.open();
    int count = 100;
    for (int i = 0; i < count; i++) {
      for (int ch = 0; ch < L2_COUNT; ++ch) {
        createAndSendMessage(ch);
      }
    }

    for (int ch = 0; ch < L2_COUNT; ++ch)
      waitForMessages(ch, count);
    
    groupChannel.close();
  }

  public void testBroadcast() throws Exception {
    setUp(0);
    groupChannel.open();
    int count = 100;
    for (int i = 0; i < count; i++) {
      groupChannel.broadcast(createMessage());
    }

    for (int ch = 0; ch < L2_COUNT; ++ch) {
      System.out.println("XXX Wait for channel " + ch);
      waitForMessages(ch, count);
    }
    
    groupChannel.close();
  }

  private PingMessage createMessage() {
    TCMessage msg = groupChannel.createMessage(TCMessageType.PING_MESSAGE);
    PingMessage ping = (PingMessage) msg;
    ping.initialize(sq);
    for (int i = 0; i < L2_COUNT; ++i)
      clientWatcheres[i].addMessageSent(ping);
    return ping;
  }

  public void testSocketInfo() throws Exception {
    setUp(0);

    assertNull(groupChannel.getRemoteAddress());
    assertNull(groupChannel.getLocalAddress());

    groupChannel.open();
    createAndSendMessage();
    waitForMessages(0, 1);

    for (int i = 0; i < L2_COUNT; ++i) {
      TCSocketAddress clientRemote = groupChannel.getChannel(groupIDs[i]).getRemoteAddress();
      TCSocketAddress clientLocal = groupChannel.getChannel(groupIDs[i]).getLocalAddress();

      MessageChannelInternal[] serverChannels = lsnr[i].getChannelManager().getChannels();
      assertEquals(1, serverChannels.length);
      MessageChannelInternal serverChannel = serverChannels[0];

      TCSocketAddress serverRemote = serverChannel.getRemoteAddress();
      TCSocketAddress serverLocal = serverChannel.getLocalAddress();

      assertEquals(clientRemote, serverLocal);
      assertEquals(clientLocal, serverRemote);
    }

    groupChannel.close();
  }

  private PingMessage createAndSendMessage() {
    return createAndSendMessage(0);
  }

  private PingMessage createAndSendMessage(int channelNum) {
    PingMessage ping = createMessage(groupIDs[channelNum]);
    clientWatcheres[channelNum].addMessageSent(ping);
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

  private ClientGroupMessageChannel createClientMessageChannel(int maxReconnectTries) {
    ConnectionAddressProvider[] addrs = new ConnectionAddressProvider[L2_COUNT];
    for (int i = 0; i < L2_COUNT; ++i) {
      addrs[i] = new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("localhost", ports[i], i) });
    }
    ClientGroupMessageChannel ch = clientComms.createClientGroupChannel(new NullSessionManager(), maxReconnectTries,
                                                                        WAIT, addrs);
    ch.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    return ch;
  }

  private PingMessage createMessage(GroupID groupID) {
    PingMessage ping = (PingMessage) groupChannel.createMessage(groupID, TCMessageType.PING_MESSAGE);
    ping.initialize(sq);
    return ping;
  }

  private void asynchCloseServers() {
    for (int i = 0; i < L2_COUNT; ++i) {
      serverComms[i].getConnectionManager().asynchCloseAllConnections();
    }
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
          public void run() {
            try {
              if (serverClose) {
                logger.info("Initiating close on the SERVER side...");
                asynchCloseServers();
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
      if (groupChannel.isConnected()) return true;
      try {
        Thread.sleep(WAIT_PERIOD);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  private void setUpClientReceiveSink(int index, ClientMessageChannel channel,
                                      MessageSendAndReceiveWatcher serverWatcher) {
    final MessageSendAndReceiveWatcher myServerSenderWatcher = serverWatcher;
    channel.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {
      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
        try {
          PingMessage ping = (PingMessage) message;
          ping.hydrate();
          // System.out.println("CLIENT-"+ndx+" RECEIVE: " + ping.getSequence());
        } catch (Exception e) {
          setError(e);
        }
        PingMessage ping = (PingMessage) message;
        myServerSenderWatcher.addMessageReceived(ping);
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

  public class MessageSendAndReceiveWatcher {

    private TLongHashSet sentSequences     = new TLongHashSet();
    private TLongHashSet receivedSequences = new TLongHashSet();

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
