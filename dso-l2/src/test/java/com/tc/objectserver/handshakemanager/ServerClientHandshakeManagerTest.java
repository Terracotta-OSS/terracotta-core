/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.StripeID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.net.protocol.tcm.TestTCMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.ObjectID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.TestLockManager;
import com.tc.object.locks.TestLockManager.ReestablishLockContext;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.l1.api.TestClientStateManager;
import com.tc.objectserver.l1.api.TestClientStateManager.AddReferenceContext;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionBatchManager;
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
  private Set                          existingUnconnectedClients;
  private TestTimer                    timer;
  private TestChannelManager           channelManager;
  private SequenceValidator            sequenceValidator;
  private TestTransactionBatchManager  transactionBatchManager;
  private InvalidateObjectManager      invalidateObjMgr;

  private static long                  SHORT_RECONNECT_TIMEOUT   = ServerClientHandshakeManager.RECONNECT_WARN_INTERVAL / 3;
  private static long                  MEDIUM_RECONNECT_TIMEOUT  = ServerClientHandshakeManager.RECONNECT_WARN_INTERVAL;
  private static long                  LONG_RECONNECT_TIMEOUT    = ServerClientHandshakeManager.RECONNECT_WARN_INTERVAL * 2;
  private static long                  DEFAULT_RECONNECT_TIMEOUT = SHORT_RECONNECT_TIMEOUT;

  @Override
  public void setUp() {
    this.existingUnconnectedClients = new HashSet();
    this.clientStateManager = new TestClientStateManager();
    this.lockManager = new TestLockManager();
    this.lockResponseSink = new TestSink();
    this.objectIDRequestSink = new TestSink();
    this.timer = new TestTimer();
    this.channelManager = new TestChannelManager();
    this.sequenceValidator = new SequenceValidator(0);
    this.transactionBatchManager = new TestTransactionBatchManager();
    this.invalidateObjMgr = Mockito.mock(InvalidateObjectManager.class);
  }

  private void initHandshakeManager(final long reconnectTimeout) {
    final TCLogger logger = TCLogging.getLogger(ServerClientHandshakeManager.class);
    this.hm = new ServerClientHandshakeManager(logger, this.channelManager, new TestServerTransactionManager(),
                                               this.transactionBatchManager, this.sequenceValidator,
                                               this.clientStateManager, this.invalidateObjMgr, this.lockManager,
                                               Mockito.mock(ServerMapEvictionManager.class), this.lockResponseSink,
                                               this.objectIDRequestSink, this.timer, reconnectTimeout, false, logger);
    this.hm.setStarting(convertToConnectionIds(this.existingUnconnectedClients));
  }

  private Set convertToConnectionIds(final Set s) {
    final HashSet ns = new HashSet();
    for (final Iterator i = s.iterator(); i.hasNext();) {
      final ClientID cid = (ClientID) i.next();
      ns.add(new ConnectionID(cid.toLong(), "FORTESTING"));
    }
    return ns;
  }

  public void testNoUnconnectedClients() throws Exception {
    initHandshakeManager(DEFAULT_RECONNECT_TIMEOUT);
    assertStarted();
  }

  public void testTimeout1() throws Exception {
    doTimeoutTest(SHORT_RECONNECT_TIMEOUT);
  }

  public void testTimeout2() throws Exception {
    doTimeoutTest(MEDIUM_RECONNECT_TIMEOUT);
  }

  public void testTimeout3() throws Exception {
    doTimeoutTest(LONG_RECONNECT_TIMEOUT);
  }

  private void doTimeoutTest(final long reconnectTimeout) throws ClientHandshakeException {
    final ClientID clientID = new ClientID(100);

    this.existingUnconnectedClients.add(clientID);
    this.existingUnconnectedClients.add(new ClientID(101));

    initHandshakeManager(reconnectTimeout);

    final TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID);
    this.hm.notifyClientConnect(handshake);

    // make sure connecting a client schedules the timer
    assertEquals(1, this.timer.scheduleCalls.size());
    final TestTimer.ScheduleCallContext scc = (ScheduleCallContext) this.timer.scheduleCalls.get(0);

    // make sure executing the timer task calls cancel on the timer and calls
    // notifyTimeout() on the handshake manager.
    assertTrue(this.timer.cancelCalls.isEmpty());
    if (reconnectTimeout < ServerClientHandshakeManager.RECONNECT_WARN_INTERVAL) {
      scc.task.run();
    } else {
      for (int i = 0; i < (reconnectTimeout / ServerClientHandshakeManager.RECONNECT_WARN_INTERVAL); i++) {
        scc.task.run();
      }
    }
    assertEquals(1, this.timer.cancelCalls.size());
    assertEquals(1, this.channelManager.closeAllChannelIDs.size());
    assertEquals(new ClientID(101), this.channelManager.closeAllChannelIDs.get(0));

    // make sure everything is started properly
    assertStarted();
  }

  public void testNotifyTimeout() throws Exception {
    final ClientID channelID1 = new ClientID(1);
    final ClientID channelID2 = new ClientID(2);

    this.existingUnconnectedClients.add(channelID1);
    this.existingUnconnectedClients.add(channelID2);

    initHandshakeManager(DEFAULT_RECONNECT_TIMEOUT);

    assertFalse(this.hm.isStarted());

    // make sure that calling notify timeout causes the remaining unconnected
    // clients to be closed.
    this.hm.notifyTimeout();
    assertEquals(2, this.channelManager.closeAllChannelIDs.size());
    assertEquals(this.existingUnconnectedClients, new HashSet(this.channelManager.closeAllChannelIDs));
    assertStarted();
  }

  public void testBasic() throws Exception {
    final Set connectedClients = new HashSet();
    final ClientID clientID1 = new ClientID(100);
    final ClientID clientID2 = new ClientID(101);
    final ClientID clientID3 = new ClientID(102);

    // channelManager.channelIDs.add(channelID1);
    // channelManager.channelIDs.add(channelID2);
    // channelManager.channelIDs.add(channelID3);

    this.existingUnconnectedClients.add(clientID1);
    this.existingUnconnectedClients.add(clientID2);

    initHandshakeManager(DEFAULT_RECONNECT_TIMEOUT);

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID1);
    final ArrayList sequenceIDs = new ArrayList();
    final SequenceID minSequenceID = new SequenceID(10);
    sequenceIDs.add(minSequenceID);
    handshake.transactionSequenceIDs = sequenceIDs;
    handshake.clientObjectIds.add(new ObjectID(200));
    handshake.clientObjectIds.add(new ObjectID(20002));
    handshake.validateObjectIds.add(new ObjectID(20002));

    final List<ClientServerExchangeLockContext> lockContexts = new LinkedList();
    lockContexts.add(new ClientServerExchangeLockContext(new StringLockID("my lock"), clientID1, new ThreadID(10001),
                                                         State.HOLDER_WRITE));
    lockContexts.add(new ClientServerExchangeLockContext(new StringLockID("my other lock)"), clientID1,
                                                         new ThreadID(10002), State.HOLDER_READ));
    final ClientServerExchangeLockContext waitContext = new ClientServerExchangeLockContext(
                                                                                            new StringLockID("d;alkjd"),
                                                                                            clientID1,
                                                                                            new ThreadID(101),
                                                                                            State.WAITER, -1);
    lockContexts.add(waitContext);
    handshake.lockContexts.addAll(lockContexts);

    handshake.isChangeListener = true;

    assertFalse(this.sequenceValidator.isNext(handshake.getSourceNodeID(), new SequenceID(minSequenceID.toLong())));
    assertEquals(2, this.existingUnconnectedClients.size());
    assertFalse(this.hm.isStarted());
    assertTrue(this.hm.isStarting());

    // reset sequence validator
    this.sequenceValidator.remove(handshake.getSourceNodeID());

    // connect the first client
    this.channelManager.clientIDs.add(handshake.clientID);
    this.hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure no state change happened.
    assertTrue(this.hm.isStarting());
    assertFalse(this.hm.isStarted());

    // make sure the timer task was scheduled properly
    assertEquals(1, this.timer.scheduleCalls.size());
    final TestTimer.ScheduleCallContext scc = (ScheduleCallContext) this.timer.scheduleCalls.get(0);
    assertEquals(Long.valueOf(DEFAULT_RECONNECT_TIMEOUT), scc.delay);
    assertTrue(scc.period == null);
    assertTrue(scc.time == null);

    // make sure the transaction sequence was set
    assertTrue(this.sequenceValidator.isNext(handshake.getSourceNodeID(), new SequenceID(minSequenceID.toLong())));

    // make sure all of the object references from that client were added to the
    // client state manager.
    assertTrue(handshake.clientObjectIds.size() > 0);
    assertEquals(handshake.clientObjectIds.size(), this.clientStateManager.addReferenceCalls.size());
    for (final Object element : this.clientStateManager.addReferenceCalls) {
      final TestClientStateManager.AddReferenceContext ctxt = (AddReferenceContext) element;
      assertTrue(handshake.clientObjectIds.remove(ctxt.objectID));
    }
    assertTrue(handshake.clientObjectIds.isEmpty());

    // make sure object validation ids are added to InvalidateObjectManager
    assertTrue(handshake.validateObjectIds.size() > 0);
    final ArgumentCaptor<Set> requestContextArg = ArgumentCaptor.forClass(Set.class);

    Mockito.verify(invalidateObjMgr, Mockito.atMost(1)).addObjectsToValidateFor((ClientID) Matchers.eq(handshake
                                                                                    .getSourceNodeID()),
                                                                                requestContextArg.capture());
    assertEquals(handshake.validateObjectIds, requestContextArg.getValue());

    // make sure outstanding locks are reestablished
    assertEquals(lockContexts.size(), handshake.lockContexts.size());
    assertEquals(getCountFor(handshake, Type.HOLDER), this.lockManager.reestablishLockCalls.size());
    int j = 0;
    for (int i = 0; i < lockContexts.size(); i++) {
      final ClientServerExchangeLockContext context = lockContexts.get(i);
      if (context.getState().getType() != Type.HOLDER) {
        continue;
      }
      final TestLockManager.ReestablishLockContext ctxt = (ReestablishLockContext) this.lockManager.reestablishLockCalls
          .get(j);
      assertEquals(context.getLockID(), ctxt.getLockContext().getLockID());
      assertEquals(context.getNodeID(), ctxt.getLockContext().getNodeID());
      assertEquals(context.getThreadID(), ctxt.getLockContext().getThreadID());
      assertEquals(context.getState().getLockLevel(), ctxt.getLockContext().getState().getLockLevel());
      j++;
    }

    // make sure the wait contexts are reestablished.
    final int waitCount = getCountFor(handshake, Type.WAITER);
    assertEquals(1, waitCount);
    assertEquals(waitCount, this.lockManager.reestablishWaitCalls.size());
    final ClientServerExchangeLockContext ctxt = ((ReestablishLockContext) this.lockManager.reestablishWaitCalls.get(0))
        .getLockContext();
    assertEquals(waitContext.getLockID(), ctxt.getLockID());
    assertEquals(waitContext.getNodeID(), ctxt.getNodeID());
    assertEquals(waitContext.getThreadID(), ctxt.getThreadID());
    assertEquals(waitContext.timeout(), ctxt.timeout());

    assertEquals(0, this.timer.cancelCalls.size());

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, this.channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(clientID2);
    this.channelManager.clientIDs.add(handshake.clientID);
    this.hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // make sure it cancels the timeout timer.
    assertEquals(1, this.timer.cancelCalls.size());

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(clientID3);
    this.channelManager.clientIDs.add(handshake.clientID);
    this.hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (final Iterator it = connectedClients.iterator(); it.hasNext();) {
      handshake = (TestClientHandshakeMessage) it.next();
      final Collection acks = this.channelManager.getMessages(handshake.clientID);
      assertEquals("Wrong number of acks for channel: " + handshake.clientID, 1, acks.size());
      final TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));
    }
  }

  private int getCountFor(final TestClientHandshakeMessage handshake, final Type type) {
    int i = 0;
    for (final Iterator<ClientServerExchangeLockContext> iterator = handshake.lockContexts.iterator(); iterator
        .hasNext();) {
      final ClientServerExchangeLockContext ctxt = iterator.next();
      if (ctxt.getState().getType() == type) {
        i++;
      }
    }
    return i;
  }

  public void testObjectIDsInHandshake() throws Exception {
    final Set connectedClients = new HashSet();
    final ClientID clientID1 = new ClientID(100);
    final ClientID clientID2 = new ClientID(101);
    final ClientID clientID3 = new ClientID(102);

    this.existingUnconnectedClients.add(clientID1);
    this.existingUnconnectedClients.add(clientID2);

    initHandshakeManager(DEFAULT_RECONNECT_TIMEOUT);

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(clientID1);
    handshake.setIsObjectIDsRequested(true);

    this.hm.notifyClientConnect(handshake);
    this.channelManager.clientIDs.add(handshake.clientID);
    connectedClients.add(handshake);

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, this.channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(clientID2);
    handshake.setIsObjectIDsRequested(false);
    this.channelManager.clientIDs.add(handshake.clientID);
    this.hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(clientID3);
    handshake.setIsObjectIDsRequested(true);
    this.channelManager.clientIDs.add(handshake.clientID);
    this.hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (final Iterator i = connectedClients.iterator(); i.hasNext();) {
      handshake = (TestClientHandshakeMessage) i.next();
      final Collection acks = this.channelManager.getMessages(handshake.clientID);
      assertEquals("Wrong number of acks for channel: " + handshake.clientID, 1, acks.size());
      final TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));
    }
  }

  private void assertStarted() {
    // make sure the lock manager got started
    assertEquals(1, this.lockManager.startCalls.size());

    // make sure the state change happens properly
    assertTrue(this.hm.isStarted());
  }

  private TestClientHandshakeMessage newClientHandshakeMessage(final ClientID clientID) {
    final TestClientHandshakeMessage handshake = new TestClientHandshakeMessage();
    handshake.clientID = clientID;
    final ArrayList sequenceIDs = new ArrayList();
    sequenceIDs.add(new SequenceID(1));
    handshake.addTransactionSequenceIDs(sequenceIDs);
    return handshake;
  }

  private static final class TestChannelManager implements DSOChannelManager {

    public final List           closeAllChannelIDs = new ArrayList();
    public final Map            handshakeMessages  = new HashMap();
    public final Set            clientIDs          = new HashSet();
    private static final String serverVersion      = "N/A";

    public void closeAll(final Collection theChannelIDs) {
      this.closeAllChannelIDs.addAll(theChannelIDs);
    }

    public MessageChannel getActiveChannel(final NodeID id) {
      return null;
    }

    public MessageChannel[] getActiveChannels() {
      return null;
    }

    public TCConnection[] getAllActiveClientConnections() {
      return null;
    }

    public String getChannelAddress(final NodeID nid) {
      return null;
    }

    public Collection getMessages(final ClientID clientID) {
      Collection msgs = (Collection) this.handshakeMessages.get(clientID);
      if (msgs == null) {
        msgs = new ArrayList();
        this.handshakeMessages.put(clientID, msgs);
      }
      return msgs;
    }

    private ClientHandshakeAckMessage newClientHandshakeAckMessage(final ClientID clientID) {
      final ClientHandshakeAckMessage msg = new TestClientHandshakeAckMessage(clientID);
      getMessages(clientID).add(msg);
      return msg;
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(final NodeID nid) {
      throw new ImplementMe();
    }

    public void addEventListener(final DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public Set getAllClientIDs() {
      return this.clientIDs;
    }

    public boolean isActiveID(final NodeID nodeID) {
      throw new ImplementMe();
    }

    public void makeChannelActive(final ClientID clientID, final boolean persistent) {
      final ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(clientID);
      ackMsg.initialize(persistent, getAllClientIDsString(), clientID, serverVersion, null, null, null);
      ackMsg.send();
    }

    private Set getAllClientIDsString() {
      final Set s = new HashSet();
      for (final Iterator i = getAllClientIDs().iterator(); i.hasNext();) {
        final ClientID cid = (ClientID) i.next();
        s.add(cid.toString());
      }
      return s;
    }

    public void makeChannelActiveNoAck(final MessageChannel channel) {
      //
    }

    public ClientID getClientIDFor(final ChannelID channelID) {
      return new ClientID(channelID.toLong());
    }

    public void makeChannelRefuse(ClientID clientID, String message) {
      throw new ImplementMe();

    }

  }

  private static class TestClientHandshakeAckMessage extends TestTCMessage implements ClientHandshakeAckMessage {
    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();
    public final ClientID               clientID;
    private boolean                     persistent;
    private final TestMessageChannel    channel;
    private String                      serverVersion;

    private TestClientHandshakeAckMessage(final ClientID clientID) {
      this.clientID = clientID;
      this.channel = new TestMessageChannel();
      this.channel.channelID = new ChannelID(clientID.toLong());
    }

    @Override
    public void send() {
      this.sendQueue.put(new Object());
    }

    public boolean getPersistentServer() {
      return this.persistent;
    }

    public void initialize(final boolean isPersistent, final Set<ClientID> allNodes, final ClientID thisNodeID,
                           final String sv, final GroupID thisGroup, final StripeID stripeID,
                           final Map<GroupID, StripeID> sidMap) {
      this.persistent = isPersistent;
      this.serverVersion = sv;
    }

    @Override
    public MessageChannel getChannel() {
      return this.channel;
    }

    public ClientID[] getAllNodes() {
      throw new ImplementMe();
    }

    public ClientID getThisNodeId() {
      throw new ImplementMe();
    }

    public String getServerVersion() {
      return this.serverVersion;
    }

    @Override
    public NodeID getSourceNodeID() {
      return this.clientID;
    }

    public GroupID getGroupID() {
      throw new ImplementMe();
    }

    public StripeID getStripeID() {
      throw new ImplementMe();
    }

    public Map<GroupID, StripeID> getStripeIDMap() {
      throw new ImplementMe();
    }

  }

}
