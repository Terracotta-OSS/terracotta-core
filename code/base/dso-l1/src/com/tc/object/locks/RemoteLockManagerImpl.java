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
  
  public RemoteLockManagerImpl(ClientIDProvider clientIdProvider, GroupID group, LockRequestMessageFactory messageFactory, ClientGlobalTransactionManager globalTxManager, ClientLockStatManager statManager) {
    this.messageFactory = messageFactory;
    this.globalTxManager = globalTxManager;
    this.group = group;
    this.clientIdProvider = clientIdProvider;
    
    this.statManager = statManager;
  }
  
  public ClientID getClientID() {
    return clientIdProvider.getClientID();
  }
  
  public void flush(LockID lock) {
    globalTxManager.flush(lock);
  }


  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    globalTxManager.waitForServerToReceiveTxnsForThisLock(lock);
  }
  
  public void interrupt(LockID lock, ThreadID thread) {
    LockRequestMessage msg = createMessage();
    msg.initializeInterruptWait(lock, thread);
    sendMessage(msg);
  }

  public boolean isTransactionsForLockFlushed(LockID lock, LockFlushCallback callback) {
    return globalTxManager.isTransactionsForLockFlushed(lock, callback);
  }

  public void lock(LockID lock, ThreadID thread, ServerLockLevel level) {
    fireRemoteCall(lock, thread);
    
    LockRequestMessage msg = createMessage();
    msg.initializeLock(lock, thread, level);
    sendMessage(msg);
  }

  public void query(LockID lock, ThreadID thread) {
    LockRequestMessage msg = createMessage();
    msg.initializeQuery(lock, thread);
    sendMessage(msg);
  }

  public void tryLock(LockID lock, ThreadID thread, ServerLockLevel level, long timeout) {
    fireRemoteCall(lock, thread);

    LockRequestMessage msg = createMessage();
    msg.initializeTryLock(lock, thread, timeout, level);
    sendMessage(msg);
  }

  public void unlock(LockID lock, ThreadID thread, ServerLockLevel level) {
    LockRequestMessage msg = createMessage();
    msg.initializeUnlock(lock, thread, level);
    sendMessage(msg);
  }

  public void wait(LockID lock, ThreadID thread, long waitTime) {
    LockRequestMessage msg = createMessage();
    msg.initializeWait(lock, thread, waitTime);
    sendMessage(msg);
  }

  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState) {
    LockRequestMessage msg = createMessage();
    msg.initializeRecallCommit(lock);
    for (ClientServerExchangeLockContext context : lockState) {
      msg.addContext(context);
    }
    sendMessage(msg);
  }

  private LockRequestMessage createMessage() {
    return messageFactory.newLockRequestMessage(group);
  }

  protected void sendMessage(LockRequestMessage msg) {
    msg.send();
  }
  
  @Deprecated
  private void fireRemoteCall(LockID lock, ThreadID thread) {
    if (statManager.isEnabled()) {
      statManager.recordLockHopped(lock, thread);
    }
  }
}
