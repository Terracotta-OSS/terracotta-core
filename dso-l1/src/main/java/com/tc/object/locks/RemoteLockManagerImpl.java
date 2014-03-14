/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.abortable.AbortedOperationException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.ClientIDProvider;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RemoteLockManagerImpl implements RemoteLockManager {
  private static final TCLogger                logger                      = TCLogging
                                                                               .getLogger(RemoteLockManagerImpl.class);

  private final static int                     MAX_BATCHED_RECALL_COMMITS  = 10000;
  private final static long                    MAX_TIME_IN_QUEUE           = 1;

  private final LockRequestMessageFactory      messageFactory;
  private final ClientGlobalTransactionManager clientGlobalTxnManager;
  private final GroupID                        group;
  private final ClientIDProvider               clientIdProvider;

  private final Queue<RecallBatchContext>      queue                       = new LinkedList<RecallBatchContext>();
  private boolean                              shutdown;

  private final Timer                          batchRecallTimer;
  private ScheduledFuture<?>                   batchRecallTask;


  public RemoteLockManagerImpl(final ClientIDProvider clientIdProvider, final GroupID group,
                               final LockRequestMessageFactory messageFactory,
                               final ClientGlobalTransactionManager clientGlobalTxnManager,
                               final TaskRunner taskRunner) {
    this.messageFactory = messageFactory;
    this.clientGlobalTxnManager = clientGlobalTxnManager;
    this.group = group;
    this.clientIdProvider = clientIdProvider;
    this.batchRecallTimer = taskRunner.newTimer("Batch Recall Timer");
  }

  @Override
  public void cleanup() {
    synchronized (queue) {
      clientGlobalTxnManager.cleanup();
      queue.clear();
    }
  }

  @Override
  public ClientID getClientID() {
    return this.clientIdProvider.getClientID();
  }

  @Override
  public void flush(final LockID lock) throws AbortedOperationException {
    this.clientGlobalTxnManager.flush(lock);
  }

  @Override
  public boolean asyncFlush(final LockID lock, final LockFlushCallback callback) {
    return this.clientGlobalTxnManager.asyncFlush(lock, callback);
  }

  @Override
  public void waitForServerToReceiveTxnsForThisLock(final LockID lock) throws AbortedOperationException {
    this.clientGlobalTxnManager.waitForServerToReceiveTxnsForThisLock(lock);
  }

  @Override
  public void interrupt(final LockID lock, final ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeInterruptWait(lock, thread);
    sendMessage(msg);
  }

  @Override
  public void lock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeLock(lock, thread, level);
    sendMessage(msg);
  }

  @Override
  public void query(final LockID lock, final ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeQuery(lock, thread);
    sendMessage(msg);
  }

  @Override
  public void tryLock(final LockID lock, final ThreadID thread, final ServerLockLevel level, final long timeout) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeTryLock(lock, thread, timeout, level);
    sendMessage(msg);
  }

  @Override
  public void unlock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeUnlock(lock, thread, level);
    sendMessage(msg);
  }

  @Override
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

  @Override
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
      if (batchRecallTask == null && !shutdown) {
        batchRecallTask = batchRecallTimer.schedule(new BatchRecallCommitsTask(),
            MAX_TIME_IN_QUEUE, TimeUnit.MILLISECONDS);
      }
    }
  }

  @Override
  public void shutdown() {
    synchronized (queue) {
      shutdown = true;
      cancelTimerTask();
    }
  }

  @Override
  public boolean isShutdown() {
    synchronized (queue) {
      return this.shutdown;
    }
  }

  private void cancelTimerTask() {
    if (batchRecallTask != null) {
      batchRecallTask.cancel(false);
    }
    batchRecallTask = null;
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

    for (final RecallBatchContext context : queue) {
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

  private class BatchRecallCommitsTask implements Runnable {
    @Override
    public void run() {
      synchronized (queue) {
        if (shutdown) {
          logger.info("Ignoring Batched Recall Requests Timer task as timer is already shut down.");
          return;
        }
        sendBatchedRequestsImmediately();
        batchRecallTask = null;
      }
    }
  }
}
