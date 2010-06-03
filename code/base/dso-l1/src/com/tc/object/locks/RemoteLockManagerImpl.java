/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.management.ClientLockStatManager;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.ClientIDProvider;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;

import java.util.Collection;

public class RemoteLockManagerImpl implements RemoteLockManager {

  private final LockRequestMessageFactory      messageFactory;
  private final ClientGlobalTransactionManager globalTxManager;
  private final GroupID                        group;
  private final ClientIDProvider               clientIdProvider;

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

  public void flush(final LockID lock, ServerLockLevel level) {
    this.globalTxManager.flush(lock, level);
  }

  public boolean asyncFlush(final LockID lock, final LockFlushCallback callback, ServerLockLevel level) {
    return this.globalTxManager.asyncFlush(lock, callback, level);
  }

  public void waitForServerToReceiveTxnsForThisLock(final LockID lock) {
    this.globalTxManager.waitForServerToReceiveTxnsForThisLock(lock);
  }

  public void interrupt(final LockID lock, final ThreadID thread) {
    final LockRequestMessage msg = createMessage();
    msg.initializeInterruptWait(lock, thread);
    sendMessage(msg);
  }

  public void lock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    fireRemoteCall(lock, thread);

    final LockRequestMessage msg = createMessage();
    msg.initializeLock(lock, thread, level);
    sendMessage(msg);
  }

  public void query(final LockID lock, final ThreadID thread) {
    final LockRequestMessage msg = createMessage();
    msg.initializeQuery(lock, thread);
    sendMessage(msg);
  }

  public void tryLock(final LockID lock, final ThreadID thread, final ServerLockLevel level, final long timeout) {
    fireRemoteCall(lock, thread);

    final LockRequestMessage msg = createMessage();
    msg.initializeTryLock(lock, thread, timeout, level);
    sendMessage(msg);
  }

  public void unlock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
    final LockRequestMessage msg = createMessage();
    msg.initializeUnlock(lock, thread, level);
    sendMessage(msg);
  }

  public void wait(final LockID lock, final ThreadID thread, final long waitTime) {
    final LockRequestMessage msg = createMessage();
    msg.initializeWait(lock, thread, waitTime);
    sendMessage(msg);
  }

  public void recallCommit(final LockID lock, final Collection<ClientServerExchangeLockContext> lockState) {
    final LockRequestMessage msg = createMessage();
    msg.initializeRecallCommit(lock);
    for (final ClientServerExchangeLockContext context : lockState) {
      msg.addContext(context);
    }
    sendMessage(msg);
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
}
