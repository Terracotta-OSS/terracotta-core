/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.impl.NullSink;
import com.tc.cluster.Cluster;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.NullPauseListener;
import com.tc.object.ObjectID;
import com.tc.object.RemoteObjectManager;
import com.tc.object.TestClientObjectManager;
import com.tc.object.dna.api.DNA;
import com.tc.object.gtx.TestClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TestRemoteTransactionManager;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.WaitInvocation;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ClientHandshakeManagerTest extends TCTestCase {
  private TestClientObjectManager            objectManager;
  private TestClientLockManager              lockManager;
  private TestChannelIDProvider              cip;
  private ClientHandshakeManager             mgr;
  private TestClientHandshakeMessageFactory  chmf;
  private TestRemoteObjectManager            remoteObjectManager;
  private TestRemoteTransactionManager       rtxManager;
  private TestClientGlobalTransactionManager gtxManager;

  public void setUp() throws Exception {
    objectManager = new TestClientObjectManager();
    remoteObjectManager = new TestRemoteObjectManager();
    lockManager = new TestClientLockManager();
    rtxManager = new TestRemoteTransactionManager();
    gtxManager = new TestClientGlobalTransactionManager();
    cip = new TestChannelIDProvider();
    chmf = new TestClientHandshakeMessageFactory();
    mgr = new ClientHandshakeManager(TCLogging.getLogger(ClientHandshakeManager.class), cip, chmf, objectManager,
                                     remoteObjectManager, lockManager, rtxManager, gtxManager, new ArrayList(),
                                     new NullSink(), new NullSessionManager(), new NullPauseListener(),
                                     new BatchSequence(new TestSequenceProvider(), 100), new Cluster());
    assertNotNull(gtxManager.pauseCalls.poll(0));
    assertNull(gtxManager.pauseCalls.poll(0));
    newMessage();
  }

  private void newMessage() {
    chmf.message = new TestClientHandshakeMessage();
  }

  public void tests() {

    Collection sequenceIDs = new ArrayList();
    sequenceIDs.add(new SequenceID(1001));
    gtxManager.transactionSequenceIDs = sequenceIDs;

    Set objectIds = new HashSet();
    for (int i = 0; i < 10; i++) {
      ObjectID id = new ObjectID(i);
      objectIds.add(id);
      objectManager.objects.put(id, new Object());
    }

    WaitLockRequest waitLockRequest = new WaitLockRequest(new LockID("1"), new ThreadID(1), LockLevel.WRITE,
                                                          new WaitInvocation());
    lockManager.outstandingWaitLockRequests.add(waitLockRequest);

    LockRequest lockRequest = new LockRequest(new LockID("2"), new ThreadID(2), LockLevel.WRITE);
    lockManager.outstandingLockAwards.add(lockRequest);

    assertTrue(lockManager.requestOutstandingContexts.isEmpty());
    assertTrue(chmf.newMessageQueue.isEmpty());

    new Thread(new Runnable() {
      public void run() {
        mgr.initiateHandshake();
      }
    }).start();

    // make sure that the lock manager was paused and unpaused.
    // assertEquals(1, lockManager.pauseContexts.size());
    lockManager.pauseContexts.take();

    TestClientHandshakeMessage sentMessage = (TestClientHandshakeMessage) chmf.newMessageQueue.take();
    assertTrue(chmf.newMessageQueue.isEmpty());

    // make sure that the manager called send on the message...
    sentMessage.sendCalls.take();
    assertTrue(sentMessage.sendCalls.isEmpty());

    // make sure the transaction sequence is retrieved and added to the message.
    assertNotNull(gtxManager.getTransactionSequenceIDsCalls.poll(0));
    assertNull(gtxManager.getTransactionSequenceIDsCalls.poll(0));
    assertEquals(gtxManager.transactionSequenceIDs, sentMessage.setTransactionSequenceIDsCalls.poll(0));

    // make sure that the manager set the expected object ids on the message...
    assertEquals(objectIds, sentMessage.clientObjectIds);

    // make sure that the manager called addAllOutstandingWaitersTo on the lock
    // manager.
    lockManager.addAllOutstandingWaitersToContexts.take();
    assertTrue(lockManager.addAllOutstandingWaitersToContexts.isEmpty());

    List waitContexts = new ArrayList(sentMessage.waitContexts);
    assertEquals(1, waitContexts.size());
    WaitContext wctxt = (WaitContext) waitContexts.get(0);
    assertEquals(waitLockRequest.lockID(), wctxt.getLockID());
    assertEquals(waitLockRequest.threadID(), wctxt.getThreadID());
    assertEquals(waitLockRequest.getWaitInvocation(), wctxt.getWaitInvocation());

    // make sure that the manager called addAllOutstandingLocksTo on the lock
    // manager.
    lockManager.addAllOutstandingLocksToContexts.take();
    assertTrue(lockManager.addAllOutstandingLocksToContexts.isEmpty());
    List lockContexts = new ArrayList(sentMessage.lockContexts);
    assertEquals(1, lockContexts.size());
    LockContext lctxt = (LockContext) lockContexts.get(0);
    assertEquals(lockRequest.lockID(), lctxt.getLockID());
    assertEquals(lockRequest.threadID(), lctxt.getThreadID());
    assertEquals(lockRequest.lockLevel(), lctxt.getLockLevel());

    // make sure that the manager called requestOutstanding() on the lock
    // manager.
    // assertEquals(1, lockManager.requestOutstandingContexts.size());
    lockManager.requestOutstandingContexts.take();
    assertTrue(lockManager.requestOutstandingContexts.isEmpty());

    // make sure the lock manager isn't unpaused until after the ACK arrives
    assertTrue(lockManager.unpauseContexts.isEmpty());
    assertTrue(gtxManager.resendOutstandingCalls.isEmpty());

    mgr.acknowledgeHandshake(0, 0, false, "1", new String[] {});

    // make sure the remote object manager was told to requestOutstanding()
    remoteObjectManager.requestOutstandingContexts.take();
    assertTrue(remoteObjectManager.requestOutstandingContexts.isEmpty());

    assertNotNull(lockManager.unpauseContexts.poll(1));
    assertNotNull(gtxManager.resendOutstandingCalls.poll(1));
  }

  private static class TestRemoteObjectManager implements RemoteObjectManager {

    public final NoExceptionLinkedQueue requestOutstandingContexts = new NoExceptionLinkedQueue();

    public DNA retrieve(ObjectID id) {
      throw new ImplementMe();
    }

    public ObjectID retrieveRootID(String name) {
      throw new ImplementMe();
    }

    public void addRoot(String name, ObjectID id) {
      throw new ImplementMe();
    }

    public void removed(ObjectID id) {
      throw new ImplementMe();
    }

    public void requestOutstanding() {
      requestOutstandingContexts.put(new Object());
    }

    public void pause() {
      return;
    }

    public void unpause() {
      return;
    }

    public void starting() {
      return;
    }

    public void addAllObjects(SessionID sessionID, long batchID, Collection dnas) {
      throw new ImplementMe();

    }

    public void clear() {
      return;
    }

    public DNA retrieve(ObjectID id, int depth) {
      throw new ImplementMe();
    }

  }

  private static class TestClientHandshakeMessageFactory implements ClientHandshakeMessageFactory {

    public TestClientHandshakeMessage   message;
    public final NoExceptionLinkedQueue newMessageQueue = new NoExceptionLinkedQueue();

    public ClientHandshakeMessage newClientHandshakeMessage() {
      newMessageQueue.put(message);
      return message;
    }

  }

  private static class TestClientLockManager implements ClientLockManager {

    public List                   outstandingLockAwards              = new LinkedList();
    public List                   outstandingWaitLockRequests        = new LinkedList();

    public NoExceptionLinkedQueue requestOutstandingContexts         = new NoExceptionLinkedQueue();
    public NoExceptionLinkedQueue addAllOutstandingWaitersToContexts = new NoExceptionLinkedQueue();
    public NoExceptionLinkedQueue addAllOutstandingLocksToContexts   = new NoExceptionLinkedQueue();
    public NoExceptionLinkedQueue pauseContexts                      = new NoExceptionLinkedQueue();
    public NoExceptionLinkedQueue unpauseContexts                    = new NoExceptionLinkedQueue();

    public void lock(LockID id, ThreadID threadID, int type) {
      return;
    }

    public void unlock(LockID id, ThreadID threadID) {
      return;
    }

    public void awardLock(SessionID sessionID, LockID id, ThreadID threadID, int type) {
      return;
    }

    public LockID lockIDFor(String id) {
      return null;
    }

    public void wait(LockID lockID, ThreadID threadID, WaitInvocation call, Object waitObject, WaitListener listener) {
      return;
    }

    public Notify notify(LockID lockID, ThreadID threadID, boolean all) {
      return Notify.NULL;
    }

    public Collection addAllPendingLockRequestsTo(Collection c) {
      requestOutstandingContexts.put(new Object());
      return c;
    }

    public void pause() {
      pauseContexts.put(new Object());
    }

    public void starting() {
      return;
    }

    public void unpause() {
      unpauseContexts.put(new Object());
    }

    public boolean isStarting() {
      return false;
    }

    public Collection addAllWaitersTo(Collection c) {
      this.addAllOutstandingWaitersToContexts.put(c);
      c.addAll(this.outstandingWaitLockRequests);
      return c;
    }

    public Collection addAllHeldLocksTo(Collection c) {
      this.addAllOutstandingLocksToContexts.put(c);
      c.addAll(this.outstandingLockAwards);
      return c;
    }

    public void notified(LockID lockID, ThreadID threadID) {
      return;
    }

    public void recall(LockID lockID, ThreadID id, int level) {
      return;
    }

    public void waitTimedOut(LockID lockID, ThreadID threadID) {
      return;
    }

    public void runGC() {
      return;
    }

    public boolean isLocked(LockID lockID) {
      return false;
    }

    public int queueLength(LockID lockID, ThreadID threadID) {
      throw new ImplementMe();
    }

    public int localHeldCount(LockID lockID, int lockLevel, ThreadID threadID) {
      throw new ImplementMe();
    }

    public boolean isLocked(LockID lockID, ThreadID threadID) {
      throw new ImplementMe();
    }

    public boolean haveLock(LockID lockID, TransactionID requesterID) {
      throw new ImplementMe();
    }

    public void queryLockCommit(ThreadID threadID, GlobalLockInfo globalLockInfo) {
      throw new ImplementMe();

    }

    public int waitLength(LockID lockID, ThreadID threadID) {
      throw new ImplementMe();
    }

    public boolean tryLock(LockID id, ThreadID threadID, int type) {
      throw new ImplementMe();
    }

    public void cannotAwardLock(SessionID sessionID, LockID id, ThreadID threadID, int type) {
      throw new ImplementMe();

    }
  }

  public class TestSequenceProvider implements BatchSequenceProvider {

    long sequence = 1;

    public synchronized void requestBatch(BatchSequenceReceiver receiver, int size) {
      receiver.setNextBatch(sequence, sequence + size);
      sequence += size;
    }

  }

}
