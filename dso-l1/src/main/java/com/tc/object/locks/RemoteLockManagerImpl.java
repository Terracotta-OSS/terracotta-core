/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.ClientLockStatManager;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.ClientIDProvider;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class RemoteLockManagerImpl implements RemoteLockManager {
  private static final TCLogger                logger                      = TCLogging
                                                                               .getLogger(RemoteLockManagerImpl.class);

  private final static int                     MAX_BATCHED_RECALL_COMMITS  = 10000;
  private final static long                    MAX_TIME_IN_QUEUE           = 1;

  private final LockRequestMessageFactory      messageFactory;
  private final ClientGlobalTransactionManager globalTxManager;
  private final GroupID                        group;
  private final ClientIDProvider               clientIdProvider;

  private final Queue<RecallBatchContext>      queue                       = new LinkedList<RecallBatchContext>();
  private BatchRecallCommitsTimerTask          batchRecallCommitsTimerTask = null;
  private final Timer                          timer                       = new Timer("Batch Recall Timer", true);
  private boolean                              shutdown                    = false;

  @Deprecated
  private final ClientLockStatManager          statManager;

  public RemoteLockManagerImpl(final ClientIDProvider clientIdProvider, final GroupID group,
                               final LockRequestMessageFactory messageFactory,
                               final ClientGlobalTransactionManager globalTxManager,
                               final ClientLockStatManager statManager) {
    this.messageFactory = messageFactory;
    this.globalTxManager = globalTxManager;
    this.group = group;
    this.clientIdProvider = clientIdProvider;

    this.statManager = statManager;
  }

  public ClientID getClientID() {
    return this.clientIdProvider.getClientID();
  }

  public void flush(final LockID lock, boolean noLocksLeftOnClient) {
    this.globalTxManager.flush(lock, noLocksLeftOnClient);
  }

  public boolean asyncFlush(final LockID lock, final LockFlushCallback callback, boolean noLocksLeftOnClient) {
    return this.globalTxManager.asyncFlush(lock, callback, noLocksLeftOnClient);
  }

  public void waitForServerToReceiveTxnsForThisLock(final LockID lock) {
    this.globalTxManager.waitForServerToReceiveTxnsForThisLock(lock);
  }

  public void interrupt(final LockID lock, final ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeInterruptWait(lock, thread);
    sendMessage(msg);
  }

  public void lock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    sendPendingRecallCommits();

    fireRemoteCall(lock, thread);

    final LockRequestMessage msg = createMessage();
    msg.initializeLock(lock, thread, level);
    sendMessage(msg);
  }

  public void query(final LockID lock, final ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeQuery(lock, thread);
    sendMessage(msg);
  }

  public void tryLock(final LockID lock, final ThreadID thread, final ServerLockLevel level, final long timeout) {
    sendPendingRecallCommits();

    fireRemoteCall(lock, thread);

    final LockRequestMessage msg = createMessage();
    msg.initializeTryLock(lock, thread, timeout, level);
    sendMessage(msg);
  }

  public void unlock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeUnlock(lock, thread, level);
    sendMessage(msg);
  }

  public void wait(final LockID lock, final ThreadID thread, final long waitTime) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeWait(lock, thread, waitTime);
    sendMessage(msg);
  }

  private void recallCommit(final LockID lock, final Collection<ClientServerExchangeLockContext> lockState) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeRecallCommit(lock);
    for (final ClientServerExchangeLockContext context : lockState) {
      msg.addContext(context);
    }
    sendMessage(msg);
  }

  public void recallCommit(final LockID lock, final Collection<ClientServerExchangeLockContext> lockState, boolean batch) {
    if (!batch) {
      recallCommit(lock, lockState);
      return;
    }

    // add it to the queue
    // check if it needs to be send immediately
    synchronized (queue) {
      queue.add(new RecallBatchContext(lockState, lock));
      if (queue.size() >= MAX_BATCHED_RECALL_COMMITS) {
        sendPendingRecallCommits();
        return;
      }
      // start a timer to send the request, if not already started
      if (batchRecallCommitsTimerTask == null && !shutdown) {
        batchRecallCommitsTimerTask = new BatchRecallCommitsTimerTask();
        timer.schedule(batchRecallCommitsTimerTask, MAX_TIME_IN_QUEUE);
      }
    }
  }

  public void shutdown() {
    synchronized (queue) {
      shutdown = true;
      timer.cancel();
    }
  }

  public boolean isShutdown() {
    synchronized (queue) {
      return this.shutdown;
    }
  }

  private void cancelTimerTask() {
    if (batchRecallCommitsTimerTask != null) {
      batchRecallCommitsTimerTask.cancel();
    }

    batchRecallCommitsTimerTask = null;
  }

  public void sendPendingRecallCommits() {
    synchronized (queue) {
      sendBatchedRequestsImmediately();
      cancelTimerTask();
    }
  }

  private void sendBatchedRequestsImmediately() {
    if (queue.size() == 0) { return; }
    // create a message and send it to the server
    LockRequestMessage lrm = createMessage();
    lrm.initializeBatchedRecallCommit();

    Iterator<RecallBatchContext> contexts = queue.iterator();
    while (contexts.hasNext()) {
      RecallBatchContext context = contexts.next();
      lrm.addRecallBatchContext(context);
    }

    queue.clear();
    sendMessage(lrm);
  }

  private LockRequestMessage createMessage() {
    return this.messageFactory.newLockRequestMessage(this.group);
  }

  protected void sendMessage(final LockRequestMessage msg) {
    msg.send();
  }

  @Deprecated
  private void fireRemoteCall(final LockID lock, final ThreadID thread) {
    if (this.statManager.isEnabled()) {
      this.statManager.recordLockHopped(lock, thread);
    }
  }

  private class BatchRecallCommitsTimerTask extends TimerTask {

    @Override
    public void run() {
      synchronized (queue) {
        if (shutdown) {
          logger.info("Ignoring Batched Recall Requests Timer task as timer is already shut down.");
          this.cancel();
          return;
        }
        sendBatchedRequestsImmediately();
        batchRecallCommitsTimerTask = null;
      }
    }
  }
}
