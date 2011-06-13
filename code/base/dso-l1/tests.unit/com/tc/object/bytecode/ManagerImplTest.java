/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dna.api.DNA;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.text.PrettyPrinter;

import java.lang.ref.WeakReference;
import java.util.Collection;

public class ManagerImplTest extends BaseDSOTestCase {

  public void testClassAutolocksIgnored() throws Exception {
    final ClientObjectManager objMgr = new ObjMgr();
    final ClientLockManager lockMgr = new LockMgr();

    final Manager manager = new ManagerImpl(false, objMgr, null, lockMgr, null, configHelper(), null);

    final LockID classLock = manager.generateLockIdentifier(getClass());
    manager.lock(classLock, LockLevel.WRITE);
    manager.unlock(classLock, LockLevel.WRITE);
  }

  private static class LockMgr implements ClientLockManager {

    public void award(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                      final ServerLockLevel level) {
      throw new ImplementMe();
    }

    public void info(final LockID lock, final ThreadID requestor,
                     final Collection<ClientServerExchangeLockContext> contexts) {
      throw new ImplementMe();

    }

    public void notified(final LockID lock, final ThreadID thread) {
      throw new ImplementMe();

    }

    public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                       final int lease) {
      throw new ImplementMe();

    }

    public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                       final int lease, boolean batch) {
      throw new ImplementMe();

    }

    public void refuse(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                       final ServerLockLevel level) {
      throw new ImplementMe();

    }

    public LockID generateLockIdentifier(final String str) {
      throw new ImplementMe();
    }

    public LockID generateLockIdentifier(final long l) {
      throw new ImplementMe();
    }

    public LockID generateLockIdentifier(final Object obj) {
      throw new ImplementMe();
    }

    public LockID generateLockIdentifier(final Object obj, final String field) {
      throw new ImplementMe();
    }

    public int globalHoldCount(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public int globalPendingCount(final LockID lock) {
      throw new ImplementMe();
    }

    public int globalWaitingCount(final LockID lock) {
      throw new ImplementMe();
    }

    public boolean isLocked(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public int localHoldCount(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public void lock(final LockID lock, final LockLevel level) {
      throw new AssertionError();
    }

    public void lockInterruptibly(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public Notify notify(final LockID lock, final Object waitObject) {
      throw new ImplementMe();
    }

    public Notify notifyAll(final LockID lock, final Object waitObject) {
      throw new ImplementMe();

    }

    public boolean tryLock(final LockID lock, final LockLevel level) {
      throw new ImplementMe();
    }

    public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) {
      throw new ImplementMe();
    }

    public void unlock(final LockID lock, final LockLevel level) {
      throw new AssertionError();
    }

    public void wait(final LockID lock, final Object waitObject) {
      throw new ImplementMe();
    }

    public void wait(final LockID lock, final Object waitObject, final long timeout) {
      throw new ImplementMe();
    }

    public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                    final ClientHandshakeMessage handshakeMessage) {
      throw new ImplementMe();
    }

    public void pause(final NodeID remoteNode, final int disconnected) {
      throw new ImplementMe();
    }

    public void shutdown() {
      throw new ImplementMe();
    }

    public void unpause(final NodeID remoteNode, final int disconnected) {
      throw new ImplementMe();
    }

    public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
      throw new ImplementMe();
    }

    public void pinLock(final LockID lock) {
      throw new ImplementMe();

    }

    public void unpinLock(final LockID lock) {
      throw new ImplementMe();

    }

    public boolean isLockedByCurrentThread(final LockLevel level) {
      throw new ImplementMe();
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      throw new ImplementMe();
    }

  }

  private static class ObjMgr implements ClientObjectManager {

    public Class getClassFor(final String className, final LoaderDescription loaderDesc) {
      throw new ImplementMe();
    }

    public boolean isManaged(final Object pojo) {
      return false;
    }

    public boolean isPortableInstance(final Object pojo) {
      throw new ImplementMe();
    }

    public boolean isPortableClass(final Class clazz) {
      throw new ImplementMe();
    }

    public void checkPortabilityOfField(final Object value, final String fieldName, final Object pojo)
        throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public void checkPortabilityOfLogicalAction(final Object[] params, final int index, final String methodName,
                                                final Object pojo) throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public Object lookupObject(final ObjectID id) {
      throw new ImplementMe();
    }

    public Object lookupObject(final ObjectID id, final ObjectID parentContext) {
      throw new ImplementMe();
    }

    public TCObject lookupOrCreate(final Object obj) {
      throw new ImplementMe();
    }

    public TCObject lookupOrCreate(final Object obj, GroupID gid) {
      throw new ImplementMe();
    }

    public ObjectID lookupExistingObjectID(final Object obj) {
      throw new ImplementMe();
    }

    public Object lookupRoot(final String name) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRoot(final String name, final Object obj) {
      throw new ImplementMe();
    }

    public TCObject lookupIfLocal(final ObjectID id) {
      throw new ImplementMe();
    }

    public TCObject lookup(final ObjectID id) {
      throw new ImplementMe();
    }

    public TCObject lookupExistingOrNull(final Object pojo) {
      return null;
    }

    public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
      throw new ImplementMe();
    }

    public WeakReference createNewPeer(final TCClass clazz, final int size, final ObjectID id, final ObjectID parentID) {
      throw new ImplementMe();
    }

    public TCClass getOrCreateClass(final Class clazz) {
      throw new ImplementMe();
    }

    public void setTransactionManager(final ClientTransactionManager txManager) {
      throw new ImplementMe();
    }

    public ClientTransactionManager getTransactionManager() {
      throw new ImplementMe();
    }

    public void shutdown() {
      throw new ImplementMe();
    }

    public void replaceRootIDIfNecessary(final String rootName, final ObjectID newRootID) {
      throw new ImplementMe();

    }

    public Object lookupOrCreateRoot(final String name, final Object obj, final boolean dsoFinal) {
      throw new ImplementMe();
    }

    public boolean isCreationInProgress() {
      throw new ImplementMe();
    }

    public Object lookupObjectNoDepth(final ObjectID id) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRootNoDepth(final String rootName, final Object object) {
      throw new ImplementMe();
    }

    public Object createOrReplaceRoot(final String rootName, final Object root) {
      throw new ImplementMe();
    }

    public void storeObjectHierarchy(final Object pojo, final ApplicationEventContext context) {
      throw new ImplementMe();
    }

    public void sendApplicationEvent(final Object pojo, final ApplicationEvent event) {
      throw new ImplementMe();
    }

    public Object cloneAndInvokeLogicalOperation(final Object pojo, final String methodName, final Object[] parameters) {
      throw new ImplementMe();
    }

    public ToggleableStrongReference getOrCreateToggleRef(final ObjectID id, final Object peer) {
      throw new ImplementMe();
    }

    public WeakReference newWeakObjectReference(final ObjectID objectID, final Object peer) {
      throw new ImplementMe();
    }

    public boolean isLocal(final ObjectID objectID) {
      throw new ImplementMe();
    }

    public void preFetchObject(final ObjectID id) {
      throw new ImplementMe();
    }
  }
}
