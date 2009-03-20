/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.config.lock.LockContextInfo;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.NodeID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TimerSpec;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.text.PrettyPrinter;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ManagerImplTest extends BaseDSOTestCase {

  public void testClassAutolocksIgnored() throws Exception {
    ClientObjectManager objMgr = new ObjMgr();
    ClientTransactionManager txnMgr = new TxnMgr();

    Manager manager = new ManagerImpl(false, objMgr, txnMgr, this.configHelper(), null);

    manager.monitorEnter(getClass(), Manager.LOCK_TYPE_WRITE, LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    manager.monitorExit(getClass());
  }

  private static class TxnMgr implements ClientTransactionManager {

    public boolean begin(final String lock, final int lockLevel, final String lockType, final String contextInfo) {
      throw new AssertionError("should not be called");
    }

    public void apply(final TxnType txType, final List lockIDs, final Collection objectChanges, final Map newRoots) {
      throw new ImplementMe();
    }

    public void createObject(final TCObject source) {
      throw new ImplementMe();
    }

    public void createRoot(final String name, final ObjectID id) {
      throw new ImplementMe();
    }

    public void fieldChanged(final TCObject source, final String classname, final String fieldname, final Object newValue, final int index) {
      throw new ImplementMe();
    }

    public void logicalInvoke(final TCObject source, final int method, final String methodName, final Object[] parameters) {
      throw new ImplementMe();
    }

    public void notify(final String lockName, final boolean all, final Object object) throws UnlockedSharedObjectException {
      throw new ImplementMe();
    }

    public void checkWriteAccess(final Object context) {
      throw new ImplementMe();
    }

    public void addReference(final TCObject tco) {
      throw new ImplementMe();
    }

    public ClientIDProvider getClientIDProvider() {
      throw new ImplementMe();
    }

    public boolean isLocked(final String lockName, final int lockLevel) {
      throw new ImplementMe();
    }

    public void commit(final String lockName) throws UnlockedSharedObjectException {
      throw new AssertionError("should not be called");
    }

    public void wait(final String lockName, final TimerSpec call, final Object object) throws UnlockedSharedObjectException {
      throw new ImplementMe();
    }

    public int queueLength(final String lockName) {
      throw new ImplementMe();
    }

    public int waitLength(final String lockName) {
      throw new ImplementMe();
    }

    public void disableTransactionLogging() {
      throw new ImplementMe();
    }

    public void enableTransactionLogging() {
      throw new ImplementMe();
    }

    public boolean isTransactionLoggingDisabled() {
      throw new ImplementMe();
    }

    public boolean isHeldByCurrentThread(final String lockName, final int lockLevel) {
      throw new ImplementMe();
    }

    public boolean beginInterruptibly(final String lockID, final int type, final String lockObjectType, final String contextInfo) {
      throw new ImplementMe();
    }

    public boolean tryBegin(final String lock, final TimerSpec timeout, final int lockLevel, final String lockType) {
      throw new ImplementMe();
    }

    public void arrayChanged(final TCObject src, final int startPos, final Object array, final int length) {
      throw new ImplementMe();
    }

    public void literalValueChanged(final TCObject source, final Object newValue, final Object oldValue) {
      throw new ImplementMe();
    }

    public void addDmiDescriptor(final DmiDescriptor d) {
      throw new ImplementMe();
    }

    public int localHeldCount(final String lockName, final int lockLevel) {
      throw new ImplementMe();
    }

    public boolean isLockOnTopStack(final String lockName) {
      return false;
    }

    public String dump() {
      throw new ImplementMe();
    }

    public void dumpToLogger() {
      throw new ImplementMe();
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      throw new ImplementMe();
    }

    public ClientTransaction getCurrentTransaction() {
      throw new ImplementMe();
    }

    public void receivedAcknowledgement(final SessionID sessionID, final TransactionID requestID, final NodeID nodeID) {
      throw new ImplementMe();
    }

    public void receivedBatchAcknowledgement(final TxnBatchID batchID, final NodeID nodeID) {
      throw new ImplementMe();
    }

    public boolean beginLockWithoutTxn(final String lockName, final int lockLevel, final String lockObjectType, final String contextInfo) {
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

    public void markReferenced(final TCObject tcobj) {
      throw new ImplementMe();
    }

    public boolean isPortableInstance(final Object pojo) {
      throw new ImplementMe();
    }

    public boolean isPortableClass(final Class clazz) {
      throw new ImplementMe();
    }

    public void checkPortabilityOfField(final Object value, final String fieldName, final Object pojo) throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public void checkPortabilityOfLogicalAction(final Object[] params, final int index, final String methodName, final Object pojo)
        throws TCNonPortableObjectError {
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

    public TCObject lookupOrShare(final Object pojo) {
      throw new ImplementMe();
    }

    public boolean isCreationInProgress() {
      throw new ImplementMe();
    }

    public void addPendingCreateObjectsToTransaction() {
      throw new ImplementMe();
    }

    public boolean hasPendingCreateObjects() {
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

    public String dump() {
      throw new ImplementMe();
    }

    public void dumpToLogger() {
      throw new ImplementMe();
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      throw new ImplementMe();
    }

    public WeakReference newWeakObjectReference(final ObjectID objectID, final Object peer) {
      throw new ImplementMe();
    }

    public boolean isLocal(final ObjectID objectID) {
      throw new ImplementMe();
    }

  }

}
