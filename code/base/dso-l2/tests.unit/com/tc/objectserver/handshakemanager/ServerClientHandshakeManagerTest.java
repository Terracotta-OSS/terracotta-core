/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.l1.api.TestClientStateManager.AddReferenceContext;
import com.tc.objectserver.lockmanager.api.TestLockManager;
import com.tc.objectserver.lockmanager.api.TestLockManager.ReestablishLockContext;
import com.tc.objectserver.lockmanager.api.TestLockManager.WaitCallContext;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;
import com.tc.util.SequenceValidator;
import com.tc.util.TestTimer;
import com.tc.util.TestTimer.ScheduleCallContext;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerClientHandshakeManagerTest extends TCTestCase {

  private ServerClientHandshakeManager hm;
  private TestClientStateManager       clientStateManager;
  private TestLockManager              lockManager;
  private TestSink                     lockResponseSink;
  private TestSink                     objectIDRequestSink;
  private long                         reconnectTimeout;
  private Set                          existingUnconnectedClients;
  private TestTimer                    timer;
  private TestChannelManager           channelManager;
  private SequenceValidator            sequenceValidator;

  public void setUp() {
    existingUnconnectedClients = new HashSet();
    clientStateManager = new TestClientStateManager();
    lockManager = new TestLockManager();
    lockResponseSink = new TestSink();
    objectIDRequestSink = new TestSink();
    reconnectTimeout = 10 * 1000;
    timer = new TestTimer();
    channelManager = new TestChannelManager();
    sequenceValidator = new SequenceValidator(0);
  }

  private void initHandshakeManager() {
    TCLogger logger = TCLogging.getLogger(ServerClientHandshakeManager.class);
    this.hm = new ServerClientHandshakeManager(logger, channelManager, new TestServerTransactionManager(),
                                               sequenceValidator, clientStateManager, lockManager, lockResponseSink,
                                               objectIDRequestSink, timer, reconnectTimeout, false, logger,
                                               NodeIDImpl.NULL_ID);
    this.hm.setStarting(convertToConnectionIds(existingUnconnectedClients));
  }

  private Set convertToConnectionIds(Set s) {
    HashSet ns = new HashSet();
    for (Iterator i = s.iterator(); i.hasNext();) {
      ClientID cid = (ClientID) i.next();
      ns.add(new ConnectionID(cid.getChannelID().toLong(), "FORTESTING"));
    }
    return ns;
  }

  public void testNoUnconnectedClients() throws Exception {
    initHandshakeManager();
    assertStarted();
  }

  public void testTimeout() throws Exception {
    ClientID clientID = new ClientID(new ChannelID(100));

    existingUnconnectedClients.add(clientID);
    existingUnconnectedClients.add(new ClientID(new ChannelID(101)));

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID);
    hm.notifyClientConnect(handshake);

    // make sure connecting a client schedules the timer
    assertEquals(1, timer.scheduleCalls.size());
    TestTimer.ScheduleCallContext scc = (ScheduleCallContext) timer.scheduleCalls.get(0);

    // make sure executing the timer task calls cancel on the timer and calls
    // notifyTimeout() on the handshake manager.
    assertTrue(timer.cancelCalls.isEmpty());
    scc.task.run();
    assertEquals(1, timer.cancelCalls.size());
    assertEquals(1, channelManager.closeAllChannelIDs.size());
    assertEquals(new ClientID(new ChannelID(101)), channelManager.closeAllChannelIDs.get(0));

    // make sure everything is started properly
    assertStarted();
  }

  public void testNotifyTimeout() throws Exception {
    ClientID channelID1 = new ClientID(new ChannelID(1));
    ClientID channelID2 = new ClientID(new ChannelID(2));

    existingUnconnectedClients.add(channelID1);
    existingUnconnectedClients.add(channelID2);

    initHandshakeManager();

    assertFalse(hm.isStarted());

    // make sure that calling notify timeout causes the remaining unconnected
    // clients to be closed.
    hm.notifyTimeout();
    assertEquals(2, channelManager.closeAllChannelIDs.size());
    assertEquals(existingUnconnectedClients, new HashSet(channelManager.closeAllChannelIDs));
    assertStarted();
  }

  public void testBasic() throws Exception {
    final Set connectedClients = new HashSet();
    ClientID clientID1 = new ClientID(new ChannelID(100));
    ClientID clientID2 = new ClientID(new ChannelID(101));
    ClientID clientID3 = new ClientID(new ChannelID(102));

    // channelManager.channelIDs.add(channelID1);
    // channelManager.channelIDs.add(channelID2);
    // channelManager.channelIDs.add(channelID3);

    existingUnconnectedClients.add(clientID1);
    existingUnconnectedClients.add(clientID2);

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID1);
    ArrayList sequenceIDs = new ArrayList();
    SequenceID minSequenceID = new SequenceID(10);
    sequenceIDs.add(minSequenceID);
    handshake.transactionSequenceIDs = sequenceIDs;
    handshake.clientObjectIds.add(new ObjectID(200));
    handshake.clientObjectIds.add(new ObjectID(20002));

    List lockContexts = new LinkedList();

    lockContexts.add(new LockContext(new LockID("my lock"), clientID1, new ThreadID(10001), LockLevel.WRITE,
                                     String.class.getName()));
    lockContexts.add(new LockContext(new LockID("my other lock)"), clientID1, new ThreadID(10002), LockLevel.READ,
                                     String.class.getName()));
    handshake.lockContexts.addAll(lockContexts);

    WaitContext waitContext = new WaitContext(new LockID("d;alkjd"), clientID1, new ThreadID(101), LockLevel.WRITE,
                                              String.class.getName(), new TimerSpec());
    handshake.waitContexts.add(waitContext);
    handshake.isChangeListener = true;

    assertFalse(sequenceValidator.isNext(handshake.getClientID(), new SequenceID(minSequenceID.toLong())));
    assertEquals(2, existingUnconnectedClients.size());
    assertFalse(hm.isStarted());
    assertTrue(hm.isStarting());

    // reset sequence validator
    sequenceValidator.remove(handshake.getClientID());

    // connect the first client
    channelManager.clientIDs.add(handshake.clientID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure no state change happened.
    assertTrue(hm.isStarting());
    assertFalse(hm.isStarted());

    // make sure the timer task was scheduled properly
    assertEquals(1, timer.scheduleCalls.size());
    TestTimer.ScheduleCallContext scc = (ScheduleCallContext) timer.scheduleCalls.get(0);
    assertEquals(new Long(reconnectTimeout), scc.delay);
    assertTrue(scc.period == null);
    assertTrue(scc.time == null);

    // make sure the transaction sequence was set
    assertTrue(sequenceValidator.isNext(handshake.getClientID(), new SequenceID(minSequenceID.toLong())));

    // make sure all of the object references from that client were added to the
    // client state manager.
    assertTrue(handshake.clientObjectIds.size() > 0);
    assertEquals(handshake.clientObjectIds.size(), clientStateManager.addReferenceCalls.size());
    for (Iterator i = clientStateManager.addReferenceCalls.iterator(); i.hasNext();) {
      TestClientStateManager.AddReferenceContext ctxt = (AddReferenceContext) i.next();
      assertTrue(handshake.clientObjectIds.remove(ctxt.objectID));
    }
    assertTrue(handshake.clientObjectIds.isEmpty());

    // make sure outstanding locks are reestablished
    assertEquals(lockContexts.size(), handshake.lockContexts.size());
    assertEquals(handshake.lockContexts.size(), lockManager.reestablishLockCalls.size());
    for (int i = 0; i < lockContexts.size(); i++) {
      LockContext lockContext = (LockContext) lockContexts.get(i);
      TestLockManager.ReestablishLockContext ctxt = (ReestablishLockContext) lockManager.reestablishLockCalls.get(i);
      assertEquals(lockContext.getLockID(), ctxt.lockContext.getLockID());
      assertEquals(lockContext.getNodeID(), ctxt.lockContext.getNodeID());
      assertEquals(lockContext.getThreadID(), ctxt.lockContext.getThreadID());
      assertEquals(lockContext.getLockLevel(), ctxt.lockContext.getLockLevel());
    }

    // make sure the wait contexts are reestablished.
    assertEquals(1, handshake.waitContexts.size());
    assertEquals(handshake.waitContexts.size(), lockManager.reestablishWaitCalls.size());
    TestLockManager.WaitCallContext ctxt = (WaitCallContext) lockManager.reestablishWaitCalls.get(0);
    assertEquals(waitContext.getLockID(), ctxt.lockID);
    assertEquals(waitContext.getNodeID(), ctxt.nid);
    assertEquals(waitContext.getThreadID(), ctxt.threadID);
    assertEquals(waitContext.getTimerSpec(), ctxt.waitInvocation);
    assertSame(lockResponseSink, ctxt.lockResponseSink);

    assertEquals(0, timer.cancelCalls.size());

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(clientID2);
    channelManager.clientIDs.add(handshake.clientID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // make sure it cancels the timeout timer.
    assertEquals(1, timer.cancelCalls.size());

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(clientID3);
    channelManager.clientIDs.add(handshake.clientID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (Iterator i = connectedClients.iterator(); i.hasNext();) {
      handshake = (TestClientHandshakeMessage) i.next();
      Collection acks = channelManager.getMessages(handshake.clientID);
      assertEquals("Wrong number of acks for channel: " + handshake.clientID, 1, acks.size());
      TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));
    }
  }

  public void testObjectIDsInHandshake() throws Exception {
    final Set connectedClients = new HashSet();
    ClientID clientID1 = new ClientID(new ChannelID(100));
    ClientID clientID2 = new ClientID(new ChannelID(101));
    ClientID clientID3 = new ClientID(new ChannelID(102));

    existingUnconnectedClients.add(clientID1);
    existingUnconnectedClients.add(clientID2);

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID1);
    handshake.setIsObjectIDsRequested(true);

    hm.notifyClientConnect(handshake);
    channelManager.clientIDs.add(handshake.clientID);
    connectedClients.add(handshake);

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(clientID2);
    handshake.setIsObjectIDsRequested(false);
    channelManager.clientIDs.add(handshake.clientID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(clientID3);
    handshake.setIsObjectIDsRequested(true);
    channelManager.clientIDs.add(handshake.clientID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (Iterator i = connectedClients.iterator(); i.hasNext();) {
      handshake = (TestClientHandshakeMessage) i.next();
      Collection acks = channelManager.getMessages(handshake.clientID);
      assertEquals("Wrong number of acks for channel: " + handshake.clientID, 1, acks.size());
      TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));
    }
  }

  private void assertStarted() {
    // make sure the lock manager got started
    assertEquals(1, lockManager.startCalls.size());

    // make sure the state change happens properly
    assertTrue(hm.isStarted());
  }

  private TestClientHandshakeMessage newClientHandshakeMessage(ClientID clientID) {
    TestClientHandshakeMessage handshake = new TestClientHandshakeMessage();
    handshake.clientID = clientID;
    ArrayList sequenceIDs = new ArrayList();
    sequenceIDs.add(new SequenceID(1));
    handshake.addTransactionSequenceIDs(sequenceIDs);
    return handshake;
  }

  private static final class TestChannelManager implements DSOChannelManager {

    public final List closeAllChannelIDs = new ArrayList();
    public final Map  handshakeMessages  = new HashMap();
    public final Set  clientIDs          = new HashSet();
    private String    serverVersion      = "N/A";

    public void closeAll(Collection theChannelIDs) {
      closeAllChannelIDs.addAll(theChannelIDs);
    }

    public MessageChannel getActiveChannel(NodeID id) {
      return null;
    }

    public MessageChannel[] getActiveChannels() {
      return null;
    }

    public Set getAllActiveClientIDs() {
      return this.clientIDs;
    }

    public boolean isValidID(ChannelID channelID) {
      return false;
    }

    public String getChannelAddress(NodeID nid) {
      return null;
    }

    public Collection getMessages(ClientID clientID) {
      Collection msgs = (Collection) this.handshakeMessages.get(clientID);
      if (msgs == null) {
        msgs = new ArrayList();
        this.handshakeMessages.put(clientID, msgs);
      }
      return msgs;
    }

    private ClientHandshakeAckMessage newClientHandshakeAckMessage(ClientID clientID) {
      ClientHandshakeAckMessage msg = new TestClientHandshakeAckMessage(clientID);
      getMessages(clientID).add(msg);
      return msg;
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
      throw new ImplementMe();
    }

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public Set getAllClientIDs() {
      return getAllActiveClientIDs();
    }

    public boolean isActiveID(NodeID nodeID) {
      throw new ImplementMe();
    }

    public void makeChannelActive(ClientID clientID, boolean persistent, NodeIDImpl serverNodeID) {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      ackMsg.initialize(persistent, getAllClientIDsString(), clientID.toString(), serverVersion, serverNodeID);
      ackMsg.send();
    }

    private Set getAllClientIDsString() {
      Set s = new HashSet();
      for (Iterator i = getAllClientIDs().iterator(); i.hasNext();) {
        ClientID cid = (ClientID) i.next();
        s.add(cid.toString());
      }
      return s;
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      //
    }

    public ClientID getClientIDFor(ChannelID channelID) {
      return new ClientID(channelID);
    }

  }

  private static class TestClientHandshakeAckMessage implements ClientHandshakeAckMessage {
    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();
    public final ClientID               clientID;
    private boolean                     persistent;
    private final TestMessageChannel    channel;
    private String                      serverVersion;
    private NodeIDImpl                  serverNodeID;

    private TestClientHandshakeAckMessage(ClientID clientID) {
      this.clientID = clientID;
      this.channel = new TestMessageChannel();
      this.channel.channelID = clientID.getChannelID();
    }

    public void send() {
      sendQueue.put(new Object());
    }

    public boolean getPersistentServer() {
      return persistent;
    }

    public void initialize(boolean isPersistent, Set allNodes, String thisNodeID, String sv, NodeIDImpl aServerNodeID) {
      this.persistent = isPersistent;
      this.serverVersion = sv;
      this.serverNodeID = aServerNodeID;
    }

    public MessageChannel getChannel() {
      return channel;
    }

    public String[] getAllNodes() {
      throw new ImplementMe();
    }

    public String getThisNodeId() {
      throw new ImplementMe();
    }

    public String getServerVersion() {
      return serverVersion;
    }

    public ClientID getClientID() {
      return this.clientID;
    }

    public NodeIDImpl getServerNodeID() {
      return serverNodeID;
    }

  }

}
