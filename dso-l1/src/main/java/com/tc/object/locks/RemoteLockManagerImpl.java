/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ClientIDProvider;
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
  private final ClientIDProvider               clientIdProvider;

  private final Queue<RecallBatchContext>      queue                       = new LinkedList<RecallBatchContext>();
  private boolean                              shutdown;

  private final Timer                          batchRecallTimer;
  private ScheduledFuture<?>                   batchRecallTask;


  public RemoteLockManagerImpl(ClientIDProvider clientIdProvider,
                               LockRequestMessageFactory messageFactory,
                               TaskRunner taskRunner) {
    this.messageFactory = messageFactory;
    this.clientIdProvider = clientIdProvider;
    this.batchRecallTimer = taskRunner.newTimer("Batch Recall Timer");
  }

  @Override
  public void cleanup() {
    synchronized (queue) {
      queue.clear();
    }
  }

  @Override
  public ClientID getClientID() {
    return this.clientIdProvider.getClientID();
  }

  @Override
  public void flush(LockID lock) {
  }

  @Override
  public boolean asyncFlush(LockID lock, LockFlushCallback callback) {
    callback.transactionsForLockFlushed(lock);
    return true;
  }

  @Override
  public void interrupt(LockID lock, ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeInterruptWait(lock, thread);
    sendMessage(msg);
  }

  @Override
  public void lock(LockID lock, ThreadID thread, ServerLockLevel level) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeLock(lock, thread, level);
    sendMessage(msg);
  }

  @Override
  public void query(LockID lock, ThreadID thread) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeQuery(lock, thread);
    sendMessage(msg);
  }

  @Override
  public void tryLock(LockID lock, ThreadID thread, ServerLockLevel level, long timeout) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeTryLock(lock, thread, timeout, level);
    sendMessage(msg);
  }

  @Override
  public void unlock(LockID lock, ThreadID thread, ServerLockLevel level) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeUnlock(lock, thread, level);
    sendMessage(msg);
  }

  @Override
  public void wait(LockID lock, ThreadID thread, long waitTime) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeWait(lock, thread, waitTime);
    sendMessage(msg);
  }

  private void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState) {
    sendPendingRecallCommits();

    final LockRequestMessage msg = createMessage();
    msg.initializeRecallCommit(lock);
    for (final ClientServerExchangeLockContext context : lockState) {
      msg.addContext(context);
    }
    sendMessage(msg);
  }

  @Override
  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState, boolean batch) {
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
    return this.messageFactory.newLockRequestMessage();
  }

  protected void sendMessage(LockRequestMessage msg) {
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
