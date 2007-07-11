/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogging;
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
import com.tc.object.tx.WaitInvocation;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.impl.TestObjectRequestManager;
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
import com.tc.util.sequence.ObjectIDSequenceProvider;

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
  private TestObjectRequestManager     objectRequestManager;
  private TestClientStateManager       clientStateManager;
  private TestLockManager              lockManager;
  private TestSink                     lockResponseSink;
  private long                         reconnectTimeout;
  private Set                          existingUnconnectedClients;
  private TestTimer                    timer;
  private TestChannelManager           channelManager;
  private SequenceValidator            sequenceValidator;
  private long                         objectIDSequenceStart;

  public void setUp() {
    existingUnconnectedClients = new HashSet();
    objectRequestManager = new TestObjectRequestManager();
    clientStateManager = new TestClientStateManager();
    lockManager = new TestLockManager();
    lockResponseSink = new TestSink();
    reconnectTimeout = 10 * 1000;
    timer = new TestTimer();
    channelManager = new TestChannelManager();
    sequenceValidator = new SequenceValidator(0);
    objectIDSequenceStart = 1000;
  }

  private void initHandshakeManager() {
    this.hm = new ServerClientHandshakeManager(TCLogging.getLogger(ServerClientHandshakeManager.class), channelManager,
                                               objectRequestManager, new TestServerTransactionManager(),
                                               sequenceValidator, clientStateManager, lockManager, lockResponseSink,
                                               new ObjectIDSequenceProvider(objectIDSequenceStart), timer,
                                               reconnectTimeout, false);
    this.hm.setStarting(convertToConnectionIds(existingUnconnectedClients));
  }

  private Set convertToConnectionIds(Set s) {
    HashSet ns = new HashSet();
    for (Iterator i = s.iterator(); i.hasNext();) {
      ChannelID cid = (ChannelID) i.next();
      ns.add(new ConnectionID(cid.toLong(), "FORTESTING"));
    }
    return ns;
  }

  public void testNoUnconnectedClients() throws Exception {
    initHandshakeManager();
    assertStarted();
  }

  public void testTimeout() throws Exception {
    ChannelID channelID1 = new ChannelID(100);

    existingUnconnectedClients.add(channelID1);
    existingUnconnectedClients.add(new ChannelID(101));

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(channelID1);
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
    assertEquals(new ChannelID(101), channelManager.closeAllChannelIDs.get(0));

    // make sure everything is started properly
    assertStarted();
  }

  public void testNotifyTimeout() throws Exception {
    ChannelID channelID1 = new ChannelID(1);
    ChannelID channelID2 = new ChannelID(2);

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
    ChannelID channelID1 = new ChannelID(100);
    ChannelID channelID2 = new ChannelID(101);
    ChannelID channelID3 = new ChannelID(102);

    // channelManager.channelIDs.add(channelID1);
    // channelManager.channelIDs.add(channelID2);
    // channelManager.channelIDs.add(channelID3);

    existingUnconnectedClients.add(channelID1);
    existingUnconnectedClients.add(channelID2);

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(channelID1);
    ArrayList sequenceIDs = new ArrayList();
    SequenceID minSequenceID = new SequenceID(10);
    sequenceIDs.add(minSequenceID);
    handshake.transactionSequenceIDs = sequenceIDs;
    handshake.clientObjectIds.add(new ObjectID(200));
    handshake.clientObjectIds.add(new ObjectID(20002));

    List lockContexts = new LinkedList();

    lockContexts.add(new LockContext(new LockID("my lock"), channelID1, new ThreadID(10001), LockLevel.WRITE));
    lockContexts.add(new LockContext(new LockID("my other lock)"), channelID1, new ThreadID(10002), LockLevel.READ));
    handshake.lockContexts.addAll(lockContexts);

    WaitContext waitContext = new WaitContext(new LockID("d;alkjd"), channelID1, new ThreadID(101), LockLevel.WRITE,
                                              new WaitInvocation());
    handshake.waitContexts.add(waitContext);
    handshake.isChangeListener = true;

    assertFalse(sequenceValidator.isNext(handshake.getChannelID(), new SequenceID(minSequenceID.toLong())));
    assertEquals(2, existingUnconnectedClients.size());
    assertFalse(hm.isStarted());
    assertTrue(hm.isStarting());

    // reset sequence validator
    sequenceValidator.remove(handshake.getChannelID());

    // connect the first client
    channelManager.channelIDs.add(handshake.channelID);
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
    assertTrue(sequenceValidator.isNext(handshake.getChannelID(), new SequenceID(minSequenceID.toLong())));

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
      assertEquals(lockContext.getChannelID(), ctxt.lockContext.getChannelID());
      assertEquals(lockContext.getThreadID(), ctxt.lockContext.getThreadID());
      assertEquals(lockContext.getLockLevel(), ctxt.lockContext.getLockLevel());
    }

    // make sure the wait contexts are reestablished.
    assertEquals(1, handshake.waitContexts.size());
    assertEquals(handshake.waitContexts.size(), lockManager.reestablishWaitCalls.size());
    TestLockManager.WaitCallContext ctxt = (WaitCallContext) lockManager.reestablishWaitCalls.get(0);
    assertEquals(waitContext.getLockID(), ctxt.lockID);
    assertEquals(waitContext.getChannelID(), ctxt.channelID);
    assertEquals(waitContext.getThreadID(), ctxt.threadID);
    assertEquals(waitContext.getWaitInvocation(), ctxt.waitInvocation);
    assertSame(lockResponseSink, ctxt.lockResponseSink);

    assertEquals(0, timer.cancelCalls.size());

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(channelID2);
    channelManager.channelIDs.add(handshake.channelID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // make sure it cancels the timeout timer.
    assertEquals(1, timer.cancelCalls.size());

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(channelID3);
    channelManager.channelIDs.add(handshake.channelID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (Iterator i = connectedClients.iterator(); i.hasNext();) {
      handshake = (TestClientHandshakeMessage) i.next();
      Collection acks = channelManager.getMessages(handshake.channelID);
      assertEquals("Wrong number of acks for channel: " + handshake.channelID, 1, acks.size());
      TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));
    }
  }

  public void testObjectIDsInHandshake() throws Exception {
    final Set connectedClients = new HashSet();
    ChannelID channelID1 = new ChannelID(100);
    ChannelID channelID2 = new ChannelID(101);
    ChannelID channelID3 = new ChannelID(102);

    existingUnconnectedClients.add(channelID1);
    existingUnconnectedClients.add(channelID2);

    initHandshakeManager();

    TestClientHandshakeMessage handshake = newClientHandshakeMessage(channelID1);
    handshake.setIsObjectIDsRequested(true);

    hm.notifyClientConnect(handshake);
    channelManager.channelIDs.add(handshake.channelID);
    connectedClients.add(handshake);

    // make sure no ack messages have been sent, since we're not started yet.
    assertEquals(0, channelManager.handshakeMessages.size());

    // connect the last outstanding client.
    handshake = newClientHandshakeMessage(channelID2);
    handshake.setIsObjectIDsRequested(false);
    channelManager.channelIDs.add(handshake.channelID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    assertStarted();

    // now that the server has started, connect a new client
    handshake = newClientHandshakeMessage(channelID3);
    handshake.setIsObjectIDsRequested(true);
    channelManager.channelIDs.add(handshake.channelID);
    hm.notifyClientConnect(handshake);
    connectedClients.add(handshake);

    // make sure that ack messages were sent for all incoming handshake messages.
    for (Iterator i = connectedClients.iterator(); i.hasNext();) {
      handshake = (TestClientHandshakeMessage) i.next();
      Collection acks = channelManager.getMessages(handshake.channelID);
      assertEquals("Wrong number of acks for channel: " + handshake.channelID, 1, acks.size());
      TestClientHandshakeAckMessage ack = (TestClientHandshakeAckMessage) new ArrayList(acks).get(0);
      assertNotNull(ack.sendQueue.poll(1));

      if (ack.channelID.equals(channelID2)) {
        assertTrue(ack.getObjectIDSequenceStart() == 0);
        assertTrue(ack.getObjectIDSequenceEnd() == 0);
      } else {
        assertFalse(ack.getObjectIDSequenceStart() == 0);
        assertFalse(ack.getObjectIDSequenceEnd() == 0);
        assertTrue(ack.getObjectIDSequenceStart() < ack.getObjectIDSequenceEnd());
      }
    }
  }

  private void assertStarted() {
    // make sure the lock manager got started
    assertEquals(1, lockManager.startCalls.size());

    // make sure the object manager got started
    assertNotNull(objectRequestManager.startCalls.poll(1));

    // make sure the state change happens properly
    assertTrue(hm.isStarted());
  }

  private TestClientHandshakeMessage newClientHandshakeMessage(ChannelID channelID1) {
    TestClientHandshakeMessage handshake = new TestClientHandshakeMessage();
    handshake.channelID = channelID1;
    ArrayList sequenceIDs = new ArrayList();
    sequenceIDs.add(new SequenceID(1));
    handshake.setTransactionSequenceIDs(sequenceIDs);
    return handshake;
  }

  private static final class TestChannelManager implements DSOChannelManager {

    public final List closeAllChannelIDs = new ArrayList();
    public final Map  handshakeMessages  = new HashMap();
    public final Set  channelIDs         = new HashSet();
    private String    serverVersion      = "N/A";

    public void closeAll(Collection theChannelIDs) {
      closeAllChannelIDs.addAll(theChannelIDs);
    }

    public MessageChannel getActiveChannel(ChannelID id) {
      return null;
    }

    public MessageChannel[] getActiveChannels() {
      return null;
    }

    public Set getAllActiveChannelIDs() {
      return this.channelIDs;
    }

    public boolean isValidID(ChannelID channelID) {
      return false;
    }

    public String getChannelAddress(ChannelID channelID) {
      return null;
    }

    public Collection getMessages(ChannelID channelID) {
      Collection msgs = (Collection) this.handshakeMessages.get(channelID);
      if (msgs == null) {
        msgs = new ArrayList();
        this.handshakeMessages.put(channelID, msgs);
      }
      return msgs;
    }

    private ClientHandshakeAckMessage newClientHandshakeAckMessage(ChannelID channelID) {
      ClientHandshakeAckMessage msg = new TestClientHandshakeAckMessage(channelID);
      getMessages(channelID).add(msg);
      return msg;
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) {
      throw new ImplementMe();
    }

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public Set getRawChannelIDs() {
      return getAllActiveChannelIDs();
    }

    public boolean isActiveID(ChannelID channelID) {
      throw new ImplementMe();
    }

    public void makeChannelActive(ChannelID channelID, long startIDs, long endIDs, boolean persistent) {
      ClientHandshakeAckMessage ackMsg = newClientHandshakeAckMessage(channelID);
      ackMsg.initialize(startIDs, endIDs, persistent, getActiveChannels(), serverVersion);
      ackMsg.send();
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      //
    }
  }

  private static class TestClientHandshakeAckMessage implements ClientHandshakeAckMessage {
    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();
    public final ChannelID              channelID;
    public long                         start;
    public long                         end;
    private boolean                     persistent;
    private final TestMessageChannel    channel;
    private String                      serverVersion;

    private TestClientHandshakeAckMessage(ChannelID channelID) {
      this.channelID = channelID;
      this.channel = new TestMessageChannel();
      this.channel.channelID = channelID;
    }

    public void send() {
      sendQueue.put(new Object());
    }

    public long getObjectIDSequenceStart() {
      return start;
    }

    public long getObjectIDSequenceEnd() {
      return end;
    }

    public boolean getPersistentServer() {
      return persistent;
    }

    public void initialize(long startOid, long endOid, boolean isPersistent, MessageChannel[] channels, String sv) {
      this.start = startOid;
      this.end = endOid;
      this.persistent = isPersistent;
      this.serverVersion = sv;
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

  }

}
