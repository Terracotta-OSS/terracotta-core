/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.ClientIDProvider;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.platform.rejoin.InternalDSCleanupHelper;
import com.tc.text.PrettyPrinter;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClientLockManagerGroupImpl extends InternalDSCleanupHelper implements ClientLockManager {
  private final Map<GroupID, ClientLockManager> lockManagers;
  private final LockDistributionStrategy        distribution;

  public ClientLockManagerGroupImpl(TCLogger logger, ClientIDProvider clientIdProvider, OrderedGroupIDs groups,
                                    LockDistributionStrategy lockDistribution, SessionManager sessionManager,
                                    ThreadIDManager threadManager, LockRequestMessageFactory messageFactory,
                                    ClientGlobalTransactionManager globalTxManager, ClientLockManagerConfig config,
                                    ClientLockStatManager statManager,
                                    AbortableOperationManager abortableOperationManager) {
    distribution = lockDistribution;
    lockManagers = new HashMap<GroupID, ClientLockManager>();

    for (GroupID g : groups.getGroupIDs()) {
      lockManagers.put(g, new ClientLockManagerImpl(logger, sessionManager, new RemoteLockManagerImpl(clientIdProvider,
                                                                                                      g,
                                                                                                      messageFactory,
                                                                                                      globalTxManager,
                                                                                                      statManager),
                                                    threadManager, config, statManager, abortableOperationManager));
    }
  }

  @Override
  public void clearInternalDS() {
    for (ClientLockManager clientLockMgr : lockManagers.values()) {
      clientLockMgr.cleanup();
    }
  }

  private ClientLockManager getClientLockManagerFor(LockID lock) {
    return lockManagers.get(distribution.getGroupIDFor(lock));
  }

  private ClientLockManager getClientLockManagerFor(GroupID group) {
    return lockManagers.get(group);
  }

  @Override
  public void lock(LockID lock, LockLevel level) throws AbortedOperationException {
    getClientLockManagerFor(lock).lock(lock, level);
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level) throws AbortedOperationException {
    return getClientLockManagerFor(lock).tryLock(lock, level);
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException,
      AbortedOperationException {
    return getClientLockManagerFor(lock).tryLock(lock, level, timeout);
  }

  @Override
  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException, AbortedOperationException {
    getClientLockManagerFor(lock).lockInterruptibly(lock, level);
  }

  @Override
  public void unlock(LockID lock, LockLevel level) throws AbortedOperationException {
    getClientLockManagerFor(lock).unlock(lock, level);
  }

  @Override
  public Notify notify(LockID lock, Object waitObject) throws AbortedOperationException {
    return getClientLockManagerFor(lock).notify(lock, null);
  }

  @Override
  public Notify notifyAll(LockID lock, Object waitObject) throws AbortedOperationException {
    return getClientLockManagerFor(lock).notifyAll(lock, null);
  }

  @Override
  public void wait(LockID lock, Object waitObject) throws InterruptedException, AbortedOperationException {
    getClientLockManagerFor(lock).wait(lock, waitObject);
  }

  @Override
  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException, AbortedOperationException {
    getClientLockManagerFor(lock).wait(lock, waitObject, timeout);
  }

  @Override
  public boolean isLocked(LockID lock, LockLevel level) throws AbortedOperationException {
    return getClientLockManagerFor(lock).isLocked(lock, level);
  }

  @Override
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) throws AbortedOperationException {
    return getClientLockManagerFor(lock).isLockedByCurrentThread(lock, level);
  }

  @Override
  public boolean isLockedByCurrentThread(LockLevel level) {
    for (ClientLockManager clm : lockManagers.values()) {
      if (clm.isLockedByCurrentThread(level)) { return true; }
    }
    return false;
  }

  @Override
  public int localHoldCount(LockID lock, LockLevel level) throws AbortedOperationException {
    return getClientLockManagerFor(lock).localHoldCount(lock, level);
  }

  @Override
  public int globalHoldCount(LockID lock, LockLevel level) throws AbortedOperationException {
    return getClientLockManagerFor(lock).globalHoldCount(lock, level);
  }

  @Override
  public int globalPendingCount(LockID lock) throws AbortedOperationException {
    return getClientLockManagerFor(lock).globalPendingCount(lock);
  }

  @Override
  public int globalWaitingCount(LockID lock) throws AbortedOperationException {
    return getClientLockManagerFor(lock).globalWaitingCount(lock);
  }

  @Override
  public void notified(LockID lock, ThreadID thread) {
    getClientLockManagerFor(lock).notified(lock, thread);
  }

  @Override
  public void recall(NodeID node, SessionID session, LockID lock, ServerLockLevel level, int lease) {
    getClientLockManagerFor(lock).recall(node, session, lock, level, lease);
  }

  @Override
  public void recall(NodeID node, SessionID session, LockID lock, ServerLockLevel level, int lease, boolean batch) {
    getClientLockManagerFor(lock).recall(node, session, lock, level, lease, batch);
  }

  @Override
  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    getClientLockManagerFor(lock).award(node, session, lock, thread, level);
  }

  @Override
  public void refuse(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    getClientLockManagerFor(lock).refuse(node, session, lock, thread, level);
  }

  @Override
  public void info(LockID lock, ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    getClientLockManagerFor(lock).info(lock, requestor, contexts);
  }

  @Override
  public void pinLock(LockID lock) {
    getClientLockManagerFor(lock).pinLock(lock);
  }

  @Override
  public void unpinLock(LockID lock) {
    getClientLockManagerFor(lock).unpinLock(lock);
  }

  @Override
  public LockID generateLockIdentifier(String str) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(long l) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(Object obj) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(Object obj, String field) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    getClientLockManagerFor((GroupID) remoteNode).initializeHandshake(thisNode, remoteNode, handshakeMessage);
  }

  @Override
  public void pause(NodeID remoteNode, int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (ClientLockManager clm : lockManagers.values()) {
        clm.pause(remoteNode, disconnected);
      }
    } else {
      ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.pause(remoteNode, disconnected);
      }
    }
  }

  @Override
  public void unpause(NodeID remoteNode, int disconnected) {
    if (remoteNode.equals(GroupID.ALL_GROUPS)) {
      for (ClientLockManager clm : lockManagers.values()) {
        clm.unpause(remoteNode, disconnected);
      }
    } else {
      ClientLockManager clm = getClientLockManagerFor((GroupID) remoteNode);
      if (clm != null) {
        clm.unpause(remoteNode, disconnected);
      }
    }
  }

  @Override
  public void shutdown() {
    for (ClientLockManager clm : lockManagers.values()) {
      clm.shutdown();
    }
  }

  @Override
  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (ClientLockManager clm : lockManagers.values()) {
      contexts.addAll(clm.getAllLockContexts());
    }
    return contexts;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.indent().print(ClientLockManagerGroupImpl.class.getSimpleName()).flush();
    for (ClientLockManager clm : lockManagers.values()) {
      out.indent().visit(clm).flush();
    }
    return out;
  }
}
