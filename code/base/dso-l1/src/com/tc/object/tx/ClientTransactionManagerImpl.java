/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.loaders.Namespace;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.session.SessionID;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author steve
 */
public class ClientTransactionManagerImpl implements ClientTransactionManager {
  private static final TCLogger          logger        = TCLogging.getLogger(ClientTransactionManagerImpl.class);

  private final ThreadLocal              transaction   = new ThreadLocal() {
                                                         protected synchronized Object initialValue() {
                                                           return new ThreadTransactionContext();
                                                         }
                                                       };

  private final ThreadLocal              txnLogging    = new ThreadLocal() {
                                                         protected Object initialValue() {
                                                           return new ThreadTransactionLoggingStack();
                                                         }

                                                       };

  private final ClientTransactionFactory txFactory;
  private final RemoteTransactionManager remoteTxManager;
  private final ClientObjectManager      objectManager;
  private final ThreadLockManager        lockManager;
  private final LiteralValues            literalValues = new LiteralValues();

  private final WaitListener             waitListener  = new WaitListener() {
                                                         public void handleWaitEvent() {
                                                           return;
                                                         }
                                                       };

  private final ChannelIDProvider        cidProvider;

  private final ClientTxMonitorMBean     txMonitor;

  public ClientTransactionManagerImpl(ChannelIDProvider cidProvider, ClientObjectManager objectManager,
                                      ThreadLockManager lockManager, ClientTransactionFactory txFactory,
                                      RemoteTransactionManager remoteTxManager, RuntimeLogger runtimeLogger,
                                      final ClientTxMonitorMBean txMonitor) {
    this.cidProvider = cidProvider;
    this.txFactory = txFactory;
    this.remoteTxManager = remoteTxManager;
    this.objectManager = objectManager;
    this.objectManager.setTransactionManager(this);
    this.lockManager = lockManager;
    this.txMonitor = txMonitor;
  }

  public int queueLength(String lockName) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    return lockManager.queueLength(lockID);
  }

  public int waitLength(String lockName) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    return lockManager.waitLength(lockID);
  }

  private int localHeldCount(String lockName, int lockLevel) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    return lockManager.localHeldCount(lockID, lockLevel);
  }

  public boolean isHeldByCurrentThread(String lockName, int lockLevel) {
    if (isTransactionLoggingDisabled()) { return true; }
    return localHeldCount(lockName, lockLevel) > 0;
  }

  public boolean isLocked(String lockName) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    return lockManager.isLocked(lockID);
  }

  public void lock(String lockName, int lockLevel) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    lockManager.lock(lockID, lockLevel);
  }

  public void unlock(String lockName) {
    final LockID lockID = lockManager.lockIDFor(lockName);
    if (lockID != null) {
      lockManager.unlock(lockID);
      getThreadTransactionContext().removeLock(lockID);
    }
  }

  public boolean tryBegin(String lockName, int lockLevel) {
    logTryBegin0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || objectManager.isCreationInProgress()) { return true; }

    final TxnType txnType = getTxnTypeFromLockLevel(lockLevel);
    ClientTransaction currentTransaction = getTransactionOrNull();

    if ((currentTransaction != null) && lockLevel == LockLevel.CONCURRENT) {
      // make formatter sane
      throw new AssertionError("Can't acquire concurrent locks in a nested lock context.");
    }

    final LockID lockID = lockManager.lockIDFor(lockName);
    boolean isLocked = lockManager.tryLock(lockID, lockLevel);
    if (!isLocked) { return isLocked; }

    pushTxContext(lockID, txnType);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(this.peekContext());
    }

    return isLocked;
  }

  public boolean begin(String lockName, int lockLevel) {
    logBegin0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || objectManager.isCreationInProgress()) { return false; }

    final TxnType txnType = getTxnTypeFromLockLevel(lockLevel);
    ClientTransaction currentTransaction = getTransactionOrNull();

    final LockID lockID = lockManager.lockIDFor(lockName);

    pushTxContext(lockID, txnType);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(this.peekContext());
    }

    lockManager.lock(lockID, lockLevel);
    return true;
  }

  private TxnType getTxnTypeFromLockLevel(int lockLevel) {
    switch (lockLevel) {
      case LockLevel.READ:
        return TxnType.READ_ONLY;
      case LockLevel.CONCURRENT:
        return TxnType.CONCURRENT;
      case LockLevel.WRITE:
        return TxnType.NORMAL;
      case LockLevel.SYNCHRONOUS_WRITE:
        return TxnType.NORMAL;
      default:
        throw Assert.failure("don't know how to translate lock level " + lockLevel);
    }
  }

  public void wait(String lockName, WaitInvocation call, Object object) throws UnlockedSharedObjectException,
      InterruptedException {
    final ClientTransaction topTxn = getTransaction();

    LockID lockID = lockManager.lockIDFor(lockName);

    if (lockID == null || lockID.isNull()) {
      lockID = topTxn.getLockID();
    }

    commit(lockID, topTxn, true);

    try {
      lockManager.wait(lockID, call, object, waitListener);
    } finally {
      createTxAndInitContext();
    }
  }

  public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException {
    final ClientTransaction currentTxn = getTransaction();
    LockID lockID = lockManager.lockIDFor(lockName);

    if (lockID == null || lockID.isNull()) {
      lockID = currentTxn.getLockID();
    }
    currentTxn.addNotify(lockManager.notify(lockID, all));
  }

  private void logTryBegin0(String lockID, int type) {
    if (logger.isDebugEnabled()) {
      logger.debug("tryBegin(): lockID=" + (lockID == null ? "null" : lockID) + ", type = " + type);
    }
  }

  private void logBegin0(String lockID, int type) {
    if (logger.isDebugEnabled()) {
      logger.debug("begin(): lockID=" + (lockID == null ? "null" : lockID) + ", type = " + type);
    }
  }

  private ClientTransaction getTransactionOrNull() {
    ThreadTransactionContext tx = getThreadTransactionContext();
    return tx.getCurrentTransaction();
  }

  private ThreadTransactionContext getThreadTransactionContext() {
    return (ThreadTransactionContext) this.transaction.get();
  }

  public ClientTransaction getTransaction() throws UnlockedSharedObjectException {
    return getTransaction(null);
  }

  private ClientTransaction getTransaction(Object context) throws UnlockedSharedObjectException {
    ClientTransaction tx = getTransactionOrNull();
    if (tx == null) {

      String type = context == null ? null : context.getClass().getName();
      String errorMsg = "Attempt to access a shared object outside the scope of a shared lock.  "
                        + "\nAll access to shared objects must be within the scope of one or more shared locks defined in your Terracotta configuration.  "
                        + "\nPlease alter the locks section of your Terracotta configuration so that this access is auto-locked or protected by a named lock.";
      String details = "";
      if (type != null) {
        details += "Shared Object Type: " + type;
      }
      throw new UnlockedSharedObjectException(errorMsg, Thread.currentThread().getName(), cidProvider.getChannelID()
          .toLong(), details);
    }
    return tx;
  }

  public void checkWriteAccess(Object context) {
    if (isTransactionLoggingDisabled()) { return; }

    // First check if we have any TXN context at all (else exception thrown)
    ClientTransaction txn = getTransaction(context);

    // make sure we're not in a read-only transaction
    // txn.readOnlyCheck();
    if (txn.getTransactionType() == TxnType.READ_ONLY) { throw new ReadOnlyException(
                                                                                     "Current transaction with read-only access attempted to modify a shared object.  "
                                                                                         + "\nPlease alter the locks section of your Terracotta configuration so that the methods involved in this transaction have read/write access.",
                                                                                     Thread.currentThread().getName(),
                                                                                     cidProvider.getChannelID()
                                                                                         .toLong()); }
  }

  /**
   * In order to support ReentrantLock, the TransactionContext that is going to be removed when doing a commit may not
   * always be at the top of a stack because an reentrant lock could issue a lock within a synchronized block (although
   * it is highly not recommended). Therefore, when a commit is issued, we need to search back the stack from the top of
   * the stack to find the appropriate TransactionContext to be removed. Most likely, the TransactionContext to be
   * removed will be on the top of the stack. Therefore, the performance should be make must difference. Only in some
   * weird situations where reentrantLock is mixed with synchronized block will the TransactionContext to be removed be
   * found otherwise.
   */
  public void commit(String lockName) throws UnlockedSharedObjectException {
    logCommit0();
    if (isTransactionLoggingDisabled() || objectManager.isCreationInProgress()) { return; }

    // ClientTransaction tx = popTransaction();
    ClientTransaction tx = getTransaction();
    LockID lockID = lockManager.lockIDFor(lockName);
    if (lockID == null || lockID.isNull()) {
      lockID = tx.getLockID();
    }
    boolean hasCommitted = commit(lockID, tx, false);

    popTransaction(lockManager.lockIDFor(lockName));

    if (peekContext() != null) {
      if (hasCommitted) {
        createTxAndInitContext();
      } else {
        // If the current transaction has not committed, we will reuse the current transaction
        // so that the current changes will have a chance to commit at the next commit point.
        tx.setTransactionContext(peekContext());
        setTransaction(tx);
      }
    }
  }

  private void createTxAndInitContext() {
    ClientTransaction ctx = txFactory.newInstance();
    ctx.setTransactionContext(peekContext());
    setTransaction(ctx);
  }

  private ClientTransaction popTransaction() {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.popCurrentTransaction();
  }

  private ClientTransaction popTransaction(LockID lockID) {
    if (lockID == null || lockID.isNull()) { return popTransaction(); }
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.popCurrentTransaction(lockID);
  }

  private TransactionContext peekContext(LockID lockID) {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.peekContext(lockID);
  }

  private TransactionContext peekContext() {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.peekContext();
  }

  private void pushTxContext(LockID lockID, TxnType txnType) {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    ttc.pushContext(lockID, txnType);
  }

  private void logCommit0() {
    if (logger.isDebugEnabled()) logger.debug("commit()");
  }

  private boolean commit(LockID lockID, ClientTransaction currentTransaction, boolean isWaitContext) {
    try {
      return commitInternal(lockID, currentTransaction, isWaitContext);
    } catch (Throwable t) {
      remoteTxManager.stopProcessing();
      Banner.errorBanner("Terracotta client shutting down due to error " + t);
      logger.error(t);
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  private boolean commitInternal(LockID lockID, ClientTransaction currentTransaction, boolean isWaitContext) {
    Assert.assertNotNull("transaction", currentTransaction);

    try {
      disableTransactionLogging();

      // If the current transactionContext is READ_ONLY, there is no need to commit.
      TransactionContext tc = peekContext(lockID);
      if (tc.getType().equals(TxnType.READ_ONLY)) {
        txMonitor.committedReadTransaction();
        return false;
      }

      boolean hasPendingCreateObjects = objectManager.hasPendingCreateObjects();
      if (hasPendingCreateObjects) {
        objectManager.addPendingCreateObjectsToTransaction();
      }

      currentTransaction.setAlreadyCommitted();
      if (currentTransaction.hasChangesOrNotifies() || hasPendingCreateObjects) {
        if (txMonitor.isEnabled()) {
          currentTransaction.updateMBean(txMonitor);
        }
        remoteTxManager.commit(currentTransaction);
      }
      return true;
    } finally {
      enableTransactionLogging();

      // always try to unlock even if we are throwing an exception
      // if (!isWaitContext && !currentTransaction.isNull()) {
      // lockManager.unlock(currentTransaction.getLockID());
      // }
      if (!isWaitContext && !currentTransaction.isNull()) {
        if (lockID != null && !lockID.isNull()) {
          lockManager.unlock(lockID);
        } else {
          throw new AssertionError("Trying to unlock with lockID = null!");
        }
      }
    }
  }

  private void basicApply(Collection objectChanges, Map newRoots, boolean force) throws DNAException {

    List l = new LinkedList();

    for (Iterator i = objectChanges.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      TCObject tcobj = null;
      Assert.assertTrue(dna.isDelta());
      try {
        // This is a major hack to prevent distributed method calls
        // sent to apps that don't have the right classes from dying
        // This should be fixed in a better way some day :-)
        objectManager.getClassFor(Namespace.parseClassNameIfNecessary(dna.getTypeName()), dna
            .getDefiningLoaderDescription());
        tcobj = objectManager.lookup(dna.getObjectID());
      } catch (ClassNotFoundException cnfe) {
        logger.warn("Could not apply change because class not local:" + dna.getTypeName());
        continue;
      }
      // Important to have a hard reference to the object while we apply
      // changes so that it doesn't get gc'd on us
      Object obj = tcobj == null ? null : tcobj.getPeerObject();
      l.add(obj);
      if (obj != null) {
        try {
          tcobj.hydrate(dna, force);
        } catch (ClassNotFoundException cnfe) {
          logger.warn("Could not apply change because class not local:" + cnfe.getMessage());
          throw new TCClassNotFoundException(cnfe);
        }
      }
    }

    for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      String rootName = (String) entry.getKey();
      ObjectID newRootID = (ObjectID) entry.getValue();
      objectManager.replaceRootIDIfNecessary(rootName, newRootID);
    }
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID transactionID) {
    this.remoteTxManager.receivedAcknowledgement(sessionID, transactionID);
  }

  public void receivedBatchAcknowledgement(TxnBatchID batchID) {
    this.remoteTxManager.receivedBatchAcknowledgement(batchID);
  }

  public void apply(TxnType txType, LockID[] lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots) {
    // beginNull(TxnType.NORMAL);
    try {
      disableTransactionLogging();
      basicApply(objectChanges, newRoots, false);
    } finally {
      // removeTopTransaction();
      enableTransactionLogging();
    }
  }

  // private void removeTopTransaction() {
  // this.getThreadTransactionContext().popCurrentTransaction();
  // }
  //
  // private void beginNull(TxnType txType) {
  // this.beginNull(LockID.NULL_ID, txType);
  // }

  // private void beginNull(LockID lockID, TxnType type) {
  // ClientTransaction current = getTransactionOrNull();
  // this.pushTxContext(lockID, type);
  // if (current == null) {
  // current = txFactory.newNullInstance(lockID, type);
  // setTransaction(current);
  // }
  // }

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();
      ClientTransaction tx = getTransaction(pojo);

      tx.literalValueChanged(source, newValue, oldValue);
    } finally {
      enableTransactionLogging();
    }

  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();

      ClientTransaction tx = getTransaction(pojo);
      logFieldChanged0(source, classname, fieldname, newValue, tx);

      if (newValue != null && literalValues.isLiteralInstance(newValue)) {
        tx.fieldChanged(source, classname, fieldname, newValue, index);
      } else {
        if (newValue != null) {
          objectManager.checkPortabilityOfField(newValue, fieldname, pojo.getClass());
        }

        TCObject tco = objectManager.lookupOrCreate(newValue);
        tx.fieldChanged(source, classname, fieldname, tco.getObjectID(), index);

        // record the reference in this transaction -- This is to solve the race condition of transactions
        // that reference objects newly "created" in other transactions that may not commit before us
        if (newValue != null) {
          tx.createObject(tco);
        }
      }
    } finally {
      enableTransactionLogging();
    }
  }

  public void arrayChanged(TCObject source, int startPos, Object array, int length) {
    if (isTransactionLoggingDisabled()) { return; }
    try {
      disableTransactionLogging();
      Object pojo = source.getPeerObject();
      ClientTransaction tx = getTransaction(pojo);

      if (!ClassUtils.isPrimitiveArray(array)) {
        Object[] objArray = (Object[]) array;
        for (int i = 0; i < length; i++) {

          Object element = objArray[i];
          if (!literalValues.isLiteralInstance(element)) {
            if (element != null) objectManager.checkPortabilityOfField(element, String.valueOf(i), pojo.getClass());

            TCObject tco = objectManager.lookupOrCreate(element);
            objArray[i] = tco.getObjectID();
            // record the reference in this transaction -- This is to solve the race condition of transactions
            // that reference objects newly "created" in other transactions that may not commit before us
            if (element != null) tx.createObject(tco);
          }
        }
      }
      tx.arrayChanged(source, startPos, array, length);

    } finally {
      enableTransactionLogging();
    }
  }

  private void logFieldChanged0(TCObject source, String classname, String fieldname, Object newValue,
                                ClientTransaction tx) {
    if (logger.isDebugEnabled()) logger.debug("fieldChanged(source=" + source + ", classname=" + classname
                                              + ", fieldname=" + fieldname + ", newValue=" + newValue + ", tx=" + tx);
  }

  public void logicalInvoke(TCObject source, int method, String methodName, Object[] parameters) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();
      ClientTransaction tx = getTransaction(pojo);

      for (int i = 0; i < parameters.length; i++) {
        Object p = parameters[i];
        boolean isLiteral = literalValues.isLiteralInstance(p);
        if (!isLiteral) {
          if (p != null) {
            objectManager.checkPortabilityOfLogicalAction(p, methodName, pojo.getClass());
          }

          TCObject tco = objectManager.lookupOrCreate(p);
          parameters[i] = tco.getObjectID();
          if (p != null) {
            // record the reference in this transaction -- This is to solve the race condition of transactions
            // that reference objects newly "created" in other transactions that may not commit before us
            tx.createObject(tco);
          }
        }
      }

      tx.logicalInvoke(source, method, parameters, methodName);
    } finally {
      enableTransactionLogging();
    }
  }

  private void setTransaction(ClientTransaction tx) {
    getThreadTransactionContext().setCurrentTransaction(tx);
  }

  public void createObject(TCObject source) {
    getTransaction().createObject(source);
  }

  public void createRoot(String name, ObjectID rootID) {
    getTransaction().createRoot(name, rootID);
  }

  public void addReference(TCObject tco) {
    ClientTransaction txn = getTransactionOrNull();
    if (txn != null) {
      txn.createObject(tco);
    }
  }

  public ChannelIDProvider getChannelIDProvider() {
    return cidProvider;
  }

  public void disableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) txnLogging.get();
    txnStack.increament();
  }

  public void enableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) txnLogging.get();
    final int size = txnStack.decrement();
    Assert.assertTrue("size=" + size, size >= 0);
  }

  public boolean isTransactionLoggingDisabled() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) txnLogging.get();
    return (txnStack.get() > 0);
  }

  public static class ThreadTransactionLoggingStack {
    int callCount = 0;

    int increament() {
      return ++callCount;
    }

    int decrement() {
      return --callCount;
    }

    int get() {
      return callCount;
    }
  }

  public void addDmiDescriptor(DmiDescriptor dd) {
    getTransaction().addDmiDescritor(dd);
  }

}
