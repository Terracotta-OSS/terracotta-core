/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.StringLockID;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.session.SessionID;
import com.tc.object.util.ReadOnlyException;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.Assert;
import com.tc.util.StringUtil;
import com.tc.util.Util;
import com.tc.util.VicariousThreadLocal;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientTransactionManagerImpl implements ClientTransactionManager, PrettyPrintable {
  private static final TCLogger                                   logger                 = TCLogging
                                                                                             .getLogger(ClientTransactionManagerImpl.class);

  private final ThreadLocal                                       transaction            = new VicariousThreadLocal() {
                                                                                           @Override
                                                                                           protected Object initialValue() {
                                                                                             return new ThreadTransactionContext();
                                                                                           }
                                                                                         };

  // We need to remove initialValue() here because read auto locking now calls Manager.isDsoMonitored() which will
  // checks if isTransactionLogging is disabled. If it runs in the context of class loading, it will try to load
  // the class ThreadTransactionContext and thus throws a LinkageError.
  private final ThreadLocal                                       txnLogging             = new VicariousThreadLocal();

  private final ClientTransactionFactory                          txFactory;
  private final RemoteTransactionManager                          remoteTxnManager;
  private final ClientObjectManager                               clientObjectManager;
  private final ClientLockManager                                 clientLockManager;

  private final ClientIDProvider                                  cidProvider;

  private final SampledCounter                                    txCounter;

  private final TCObjectSelfStore                                 tcObjectSelfStore;
  private final AbortableOperationManager                         abortableOperationManager;
  private volatile int                                            session                = 0;
  private final Map<LogicalChangeID, LogicalChangeResultCallback> logicalChangeCallbacks = new ConcurrentHashMap<LogicalChangeID, LogicalChangeResultCallback>();
  private final Sequence                                          logicalChangeSequence  = new SimpleSequence();
  private final ReadWriteLock                                     rejoinCleanupLock      = new ReentrantReadWriteLock();
  private static final StringLockID                               CAS_LOCK_ID            = new StringLockID(
                                                                                                            "__eventual_lock_for_sync_logical_Invoke");

  public ClientTransactionManagerImpl(final ClientIDProvider cidProvider,
                                      final ClientObjectManager clientObjectManager,
                                      final ClientTransactionFactory txFactory,
                                      final ClientLockManager clientLockManager,
                                      final RemoteTransactionManager remoteTxManager, final SampledCounter txCounter,
                                      TCObjectSelfStore tcObjectSelfStore,
                                      AbortableOperationManager abortableOperationManager) {
    this.cidProvider = cidProvider;
    this.txFactory = txFactory;
    this.clientLockManager = clientLockManager;
    this.remoteTxnManager = remoteTxManager;
    this.clientObjectManager = clientObjectManager;
    this.clientObjectManager.setTransactionManager(this);
    this.txCounter = txCounter;
    this.tcObjectSelfStore = tcObjectSelfStore;
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void cleanup() {
    // remoteTxnManager will be cleanup from clientHandshakeCallbacks
    // clientObjectManager can't because this call is from ClientObjectManagerImpl
    // clientLockManager will be cleanup from clientHandshakeCallbacks
    // tcObjectSelfStore (or L1ServerMapLocalCacheManager) will be cleanup from L1ServerMapLocalCacheManagerImpl
    rejoinCleanupLock.writeLock().lock();
    try {
      session++;
      for (LogicalChangeResultCallback logicalChangeCallback : logicalChangeCallbacks.values()) {
        // wake up the waiting threads for LogicalChangeResult
        logicalChangeCallback.cleanup();
      }
      logicalChangeCallbacks.clear();
    } finally {
      rejoinCleanupLock.writeLock().unlock();
    }
  }

  @Override
  public void begin(final LockID lock, final LockLevel lockLevel, boolean atomic) {
    logBegin0(lock, lockLevel);

    if (isTransactionLoggingDisabled() || this.clientObjectManager.isCreationInProgress()) { return; }

    final TxnType transactionType = getTxnTypeFromLockLevel(lockLevel);
    if (transactionType == null) { return; }

    final TransactionContext context = getThreadTransactionContext().peekContext(lock);
    if ((context != null) && context.getLockType().isConcurrent()) {
      // NOTE: when removing this restriction, there are other places to clean up:
      // 1) ClientTransactionManagerImpl.apply()
      // 2) DNAFactory.flushDNAFor(LockID)
      // 3) RemoteTransactionManagerImpl.commit()
      // 4) ClientLock.removeCurrent()
      throw new UnsupportedOperationException("Don't currently support nested concurrent write transactions");
    }

    final ClientTransaction currentTransaction = getTransactionOrNull();

    if (atomic && currentTransaction != null && currentTransaction.isAtomic()) { throw new UnsupportedOperationException(
                                                                                                                         "Nested Atomic Transactons are not supported"); }
    pushTxContext(currentTransaction, lock, transactionType);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(peekContext());
    }
    if (atomic) {
      getCurrentTransaction().setAtomic(true);
    }
  }

  private TxnType getTxnTypeFromLockLevel(final LockLevel lockLevel) {
    switch (lockLevel) {
      case CONCURRENT:
        return TxnType.CONCURRENT;
      case SYNCHRONOUS_WRITE:
        return TxnType.SYNC_WRITE;
      case WRITE:
        return TxnType.NORMAL;
      default:
        return null;
    }
  }

  @Override
  public void notify(final Notify notify) throws UnlockedSharedObjectException {
    final ClientTransaction currentTxn = getTransactionOrNull();

    if (currentTxn == null
        || (currentTxn.getEffectiveType() != TxnType.NORMAL && currentTxn.getEffectiveType() != TxnType.SYNC_WRITE)) { throw new IllegalMonitorStateException(
                                                                                                                                                              getIllegalMonitorStateExceptionMessage()); }

    currentTxn.addNotify(notify);
  }

  private String getIllegalMonitorStateExceptionMessage() {
    final StringBuffer errorMsg = new StringBuffer(
                                                   "An IllegalMonitorStateException is usually caused by one of the following:");
    errorMsg.append(StringUtil.LINE_SEPARATOR);
    errorMsg.append("1) No synchronization");
    errorMsg.append(StringUtil.LINE_SEPARATOR);
    errorMsg.append("2) The object synchronized is not the same as the object waited/notified");
    errorMsg.append(StringUtil.LINE_SEPARATOR);
    errorMsg
        .append("3) The object being waited/notified on is a Terracotta distributed object, but no Terracotta auto-lock has been specified.");
    errorMsg.append(StringUtil.LINE_SEPARATOR);
    errorMsg.append("4) Read-level or named locks are being used where write-level locks or autolocks are necessary.");
    errorMsg.append(StringUtil.LINE_SEPARATOR);
    errorMsg.append("5) A lock has been specified but was applied to an object before that object was shared.");
    errorMsg.append(StringUtil.LINE_SEPARATOR).append(StringUtil.LINE_SEPARATOR);

    return Util.getFormattedMessage(errorMsg.toString());
  }

  private void logBegin0(final LockID lock, final LockLevel level) {
    if (logger.isDebugEnabled()) {
      logger.debug("begin(): lock =" + lock + ", level = " + level);
    }
  }

  private ClientTransaction getTransactionOrNull() {
    final ThreadTransactionContext tx = getThreadTransactionContext();
    return tx.getCurrentTransaction();
  }

  private ThreadTransactionContext getThreadTransactionContext() {
    return (ThreadTransactionContext) this.transaction.get();
  }

  private ClientTransaction getTransaction() throws UnlockedSharedObjectException {
    return getTransaction(null);
  }

  private ClientTransaction getTransaction(final Object context) throws UnlockedSharedObjectException {
    final ClientTransaction tx = getTransactionOrNull();
    if (tx == null) {
      handleUnlockedObjectException(context);
    }
    return tx;
  }

  private void handleUnlockedObjectException(final Object context) {
    final String type = context == null ? null : context.getClass().getName();
    final String errorMsg = "Attempt to access a shared object outside the scope of a shared lock.\n"
                            + "All access to shared objects must be within the scope of one or more\n"
                            + "shared locks defined in your Terracotta configuration.";
    String details = "";
    if (type != null) {
      details += "Shared Object Type: " + type;
    }
    details += "\n\nThe cause may be one or more of the following:\n"
               + " * Terracotta locking was not configured for the shared code.\n"
               + " * The code itself does not have synchronization that Terracotta\n" + "   can use as a boundary.\n"
               + " * The class doing the locking must be included for instrumentation.\n"
               + " * The object was first locked, then shared.\n\n"
               + "For more information on how to solve this issue, see:\n"
               + UnlockedSharedObjectException.TROUBLE_SHOOTING_GUIDE;

    throw new UnlockedSharedObjectException(errorMsg, Thread.currentThread().getName(), this.cidProvider.getClientID()
        .toLong(), details);
  }

  /**
   * In order to support ReentrantLock, the TransactionContext that is going to be removed when doing a commit may not
   * always be at the top of a stack because an reentrant lock could issue a lock within a synchronized block (although
   * it is highly not recommended). Therefore, when a commit is issued, we need to search back the stack from the top of
   * the stack to find the appropriate TransactionContext to be removed. Most likely, the TransactionContext to be
   * removed will be on the top of the stack. Therefore, the performance should be make must difference. Only in some
   * weird situations where reentrantLock is mixed with synchronized block will the TransactionContext to be removed be
   * found otherwise.
   * 
   * @throws AbortedOperationException
   */
  @Override
  public void commit(final LockID lock, final LockLevel level, boolean atomic, OnCommitCallable onCommitCallable)
      throws UnlockedSharedObjectException, AbortedOperationException {
    logCommit0();
    if (isTransactionLoggingDisabled() || this.clientObjectManager.isCreationInProgress()) { return; }

    final TxnType transactionType = getTxnTypeFromLockLevel(level);
    if (transactionType == null) {
      call(onCommitCallable);
      return;
    }

    final ClientTransaction tx = getTransactionOrNull();
    if (tx == null) {
      call(onCommitCallable);
      handleUnlockedObjectException(null);
    }
    if (atomic && !tx.isAtomic()) {
      call(onCommitCallable);
      throw new IllegalStateException(
                                      "Trying to commit a transaction atomically when current transaction is not atomic");
    }

    if (!atomic && tx.isAtomic()) {
      if (transactionType.isConcurrent()) {
        popLockContext(lock);
        call(onCommitCallable);
        return;
      }
      // add the txnCommitCallable and return If not an atomic commit and current txn is atomic
      tx.addOnCommitCallable(getOnCommitCallableForAtomicTxn(lock, onCommitCallable));
      return;
    } else {
      try {
        // commit and call OnCommitCallable callback inline which call lockManager.unlock
        commit(lock, tx);
      } finally {
        call(onCommitCallable);
      }
    }
  }

  private void call(final OnCommitCallable onCommitCallable) throws AbortedOperationException {
    if (onCommitCallable != null) {
      onCommitCallable.call();
    }
  }

  private OnCommitCallable getOnCommitCallableForAtomicTxn(final LockID lock, final OnCommitCallable delegate) {
    return new OnCommitCallable() {

      @Override
      public void call() throws AbortedOperationException {
        popTransaction(lock);
        ClientTransactionManagerImpl.this.call(delegate);
      }
    };
  }

  private void notifyTransactionAborted(ClientTransaction tx) {
    List<TransactionCompleteListener> listeners = tx.getTransactionCompleteListeners();
    TransactionID tid = tx.getTransactionID();
    for (TransactionCompleteListener listener : listeners) {
      listener.transactionAborted(tid);
    }
  }

  private void notifyTransactionCompleted(ClientTransaction tx) {
    List<TransactionCompleteListener> listeners = tx.getTransactionCompleteListeners();
    TransactionID tid = tx.getTransactionID();
    for (TransactionCompleteListener listener : listeners) {
      listener.transactionComplete(tid);
    }
  }

  private void createTxAndInitContext() {
    final ClientTransaction ctx = this.txFactory.newInstance(this.session);
    ctx.setTransactionContext(peekContext());
    setTransaction(ctx);
  }

  private ClientTransaction popTransaction(final LockID lockID) {
    final ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.popCurrentTransaction(lockID);
  }

  private void popLockContext(final LockID lockID) {
    final ThreadTransactionContext ttc = getThreadTransactionContext();
    ttc.popLockContext(lockID);
  }

  private TransactionContext peekContext() {
    final ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.peekContext();
  }

  private void pushTxContext(final ClientTransaction currentTransaction, final LockID lockID, final TxnType type) {
    final ThreadTransactionContext ttc = getThreadTransactionContext();
    ttc.pushContext(lockID, type, type);
  }

  private void logCommit0() {
    if (logger.isDebugEnabled()) {
      logger.debug("commit()");
    }
  }

  private void commit(final LockID lock, final ClientTransaction tx) throws AbortedOperationException {
    boolean rejoinInProgress = false;
    boolean hasCommitted = false;
    boolean aborted = false;
    try {
      // Check here that If operation was already aborted.
      AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
      if (this.session != tx.getSession()) { throw new PlatformRejoinException(
                                                                               "unable to commit transaction as rejoin occured"); }
      hasCommitted = commitInternal(lock, tx);
    } catch (AbortedOperationException t) {
      aborted = true;
    } catch (PlatformRejoinException t) {
      rejoinInProgress = true;
    }

    popTransaction(lock);
    for (OnCommitCallable callable : tx.getOnCommitCallables()) {
      try {
        callable.call();
      } catch (AbortedOperationException e) {
        // We need to call all the onCommitCallables even If we get aborted otherwise some locks will remain stuck.
        aborted = true;
      } catch (PlatformRejoinException e) {
        rejoinInProgress = true;
      }
    }

    if (peekContext() != null) {
      if (hasCommitted || aborted || rejoinInProgress) {
        createTxAndInitContext();
      } else {
        // If the current transaction has not committed, we will reuse the current transaction
        // so that the current changes will have a chance to commit at the next commit point.
        tx.setTransactionContext(peekContext());
        setTransaction(tx);
      }
    }
    if (aborted || rejoinInProgress) {
      notifyTransactionAborted(tx);
      // If aborted and transaction is not empty then
      // throw AbortedOperationException
      if (aborted && tx.hasChangesOrNotifies()) { throw new AbortedOperationException(); }
      if (rejoinInProgress && tx.hasChangesOrNotifies()) { throw new PlatformRejoinException(); }
    }

  }

  private boolean commitInternal(final LockID lockID, final ClientTransaction currentTransaction)
      throws AbortedOperationException {
    Assert.assertNotNull("transaction", currentTransaction);

    try {
      disableTransactionLogging();

      currentTransaction.setAlreadyCommitted();

      if (currentTransaction.hasChangesOrNotifies()) {
        this.txCounter.increment();
        this.remoteTxnManager.commit(currentTransaction);
      } else {
        // notify completion listeners on txn completion.
        notifyTransactionCompleted(currentTransaction);
      }
      return true;
    } finally {
      enableTransactionLogging();
    }
  }

  private void basicApply(final Collection objectChanges, final Map newRoots, final boolean force) throws DNAException {
    final List l = new LinkedList();

    for (final Iterator i = objectChanges.iterator(); i.hasNext();) {
      final DNA dna = (DNA) i.next();
      TCObject tcobj = null;
      Assert.assertTrue(dna.isDelta());

      try {
        tcobj = this.clientObjectManager.lookupQuiet(dna.getObjectID());
      } catch (final ClassNotFoundException cnfe) {
        logger.warn("Could not apply change because class not local: " + dna.getTypeName());
        continue;
      } catch (AbortedOperationException e) {
        // Called from Stage Thread. Never Throw aborted exception
        throw new TCRuntimeException(e);
      }
      // Important to have a hard reference to the object while we apply
      // changes so that it doesn't get gc'd on us
      final Object obj = tcobj == null ? null : tcobj.getPeerObject();
      l.add(obj);
      if (obj != null) {
        try {
          if (tcobj instanceof TCObjectSelf) {
            // DEV-6384: Applying a transaction currently doesn't work so well due
            // to the byte array getting nulled breaking replace. So instead, we'll be
            // just immediately invalidating the object upon receiving a transaction.
            this.tcObjectSelfStore.removeObjectById(tcobj.getObjectID());
          } else {
            tcobj.hydrate(dna, force, null);
          }
        } catch (final ClassNotFoundException cnfe) {
          logger.warn("Could not apply change because class not local: " + cnfe.getMessage());
          throw new TCClassNotFoundException(cnfe);
        }
      }
    }

    for (final Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final String rootName = (String) entry.getKey();
      final ObjectID newRootID = (ObjectID) entry.getValue();
      this.clientObjectManager.replaceRootIDIfNecessary(rootName, newRootID);
    }
  }

  @Override
  public void receivedAcknowledgement(final SessionID sessionID, final TransactionID transactionID, final NodeID nodeID) {
    this.remoteTxnManager.receivedAcknowledgement(sessionID, transactionID, nodeID);
  }

  @Override
  public void receivedBatchAcknowledgement(final TxnBatchID batchID, final NodeID nodeID) {
    this.remoteTxnManager.receivedBatchAcknowledgement(batchID, nodeID);
  }

  @Override
  public void apply(final TxnType txType, final List<LockID> lockIDs, final Collection objectChanges, Map newRoots) {
    try {
      disableTransactionLogging();
      basicApply(objectChanges, newRoots, false);
    } finally {
      enableTransactionLogging();
    }
  }

  @Override
  public void logicalInvoke(final TCObject source, final LogicalOperation method, final Object[] parameters) {
    logicalInvoke(source, method, parameters, LogicalChangeID.NULL_ID);
  }

  private void logicalInvoke(final TCObject source, final LogicalOperation method,
                             final Object[] parameters, LogicalChangeID id) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      final Object pojo = source.getPeerObject();
      ClientTransaction tx;

      try {
        tx = getTransaction(pojo);
      } catch (final UnlockedSharedObjectException usoe) {
        throw checkAndReportUnlockedSharedObjectException(usoe, "Failed Method Call: " + method, pojo, parameters);
      }

      for (int i = 0; i < parameters.length; i++) {
        final Object p = parameters[i];
        final boolean isLiteral = LiteralValues.isLiteralInstance(p);
        if (!isLiteral) {
          if (p != null) {
            this.clientObjectManager.checkPortabilityOfLogicalAction(method, parameters, i, pojo);
          }

          final TCObject tco = this.clientObjectManager.lookupOrCreate(p);
          parameters[i] = tco.getObjectID();
          if (p != null) {
            // record the reference in this transaction -- This is to solve the race condition of transactions
            // that reference objects newly "created" in other transactions that may not commit before us
            tx.createObject(tco);
          }
        }
      }
      tx.logicalInvoke(source, method, parameters, id);
    } finally {
      enableTransactionLogging();
    }
  }

  private RuntimeException checkAndReportUnlockedSharedObjectException(final UnlockedSharedObjectException usoe,
                                                                       final String details, Object context,
                                                                       final Object[] parameters) {
    if (this.clientLockManager.isLockedByCurrentThread(LockLevel.READ)) {
      final ReadOnlyException roe = makeReadOnlyException(details);
      return roe;
    } else {
      return usoe;
    }
  }

  private ReadOnlyException makeReadOnlyException(final String details) {
    final long vmId = this.cidProvider.getClientID().toLong();

    final ReadOnlyException roe;

    if (details != null) {
      roe = new ReadOnlyException(READ_ONLY_TEXT, Thread.currentThread().getName(), vmId, details);
    } else {
      roe = new ReadOnlyException(READ_ONLY_TEXT, Thread.currentThread().getName(), vmId);
    }
    System.err.println(roe.getMessage());
    return roe;
  }

  private void setTransaction(final ClientTransaction tx) {
    getThreadTransactionContext().setCurrentTransaction(tx);
  }

  @Override
  public void createObject(final TCObject source) {
    getTransaction().createObject(source);
  }

  @Override
  public void createRoot(final String name, final ObjectID rootID) {
    getTransaction().createRoot(name, rootID);
  }

  @Override
  public ClientTransaction getCurrentTransaction() {
    return getTransactionOrNull();
  }

  @Override
  public void disableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    if (txnStack == null) {
      txnStack = new ThreadTransactionLoggingStack();
      this.txnLogging.set(txnStack);
    }
    txnStack.increment();
  }

  @Override
  public void enableTransactionLogging() {
    final ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    Assert.assertNotNull(txnStack);
    final int size = txnStack.decrement();

    if (size < 0) { throw new AssertionError("size=" + size); }
  }

  @Override
  public boolean isTransactionLoggingDisabled() {
    final Object txnStack = this.txnLogging.get();
    return (txnStack != null) && (((ThreadTransactionLoggingStack) txnStack).get() > 0);
  }

  public static class ThreadTransactionLoggingStack {
    int callCount = 0;

    public int increment() {
      return ++this.callCount;
    }

    public int decrement() {
      return --this.callCount;
    }

    public int get() {
      return this.callCount;
    }
  }

  @Override
  public void addMetaDataDescriptor(final TCObject tco, final MetaDataDescriptorInternal md) {
    md.setObjectID(tco.getObjectID());
    getTransaction().addMetaDataDescriptor(tco, md);
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(getClass().getName());
    return out;
  }

  private static final String READ_ONLY_TEXT = "Attempt to write to a shared object inside the scope of a lock declared as a"
                                               + "\nread lock. All writes to shared objects must be within the scope of one or"
                                               + "\nmore shared locks with write access defined in your Terracotta configuration."
                                               + "\n\nPlease alter the locks section of your Terracotta configuration so that this"
                                               + "\naccess is auto-locked or protected by a named lock with write access."
                                               + "\n\nFor more information on this issue, please visit our Troubleshooting Guide at:"
                                               + "\n" + UnlockedSharedObjectException.TROUBLE_SHOOTING_GUIDE;

  @Override
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    this.remoteTxnManager.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public void receivedLogicalChangeResult(Map<LogicalChangeID, LogicalChangeResult> results) {
    for (Entry<LogicalChangeID, LogicalChangeResult> entry : results.entrySet()) {
      LogicalChangeResultCallback listener = this.logicalChangeCallbacks.remove(entry.getKey());
      if (listener != null) { // If NonStopException already removed the listener
        listener.handleResult(entry.getValue());
      } else {
        logger.warn("LogicalChangeResultCallback not present for- " + entry.getKey());
      }
    }
  }

  @Override
  public boolean logicalInvokeWithResult(final TCObject source, final LogicalOperation method,
                                         final Object[] parameters) throws AbortedOperationException {
    if (getTransaction().isAtomic()) { throw new UnsupportedOperationException(
                                                                               "LogicalInvokeWithResult not supported for atomic transactions"); }
    LogicalChangeID id = getNextLogicalChangeId();
    LogicalChangeResultCallback future = createLogicalChangeFuture(id);
    try {
      begin(CAS_LOCK_ID, LockLevel.CONCURRENT, false);
      try {
        logicalInvoke(source, method, parameters, id);
      } finally {
        commit(CAS_LOCK_ID, LockLevel.CONCURRENT, false, null);
      }
      return future.getResult();
    } finally {
      logicalChangeCallbacks.remove(id);
    }
  }

  LogicalChangeID getNextLogicalChangeId() {
    return new LogicalChangeID(logicalChangeSequence.next());
  }

  private LogicalChangeResultCallback createLogicalChangeFuture(LogicalChangeID id) {
    rejoinCleanupLock.readLock().lock();
    try {
      LogicalChangeResultCallback future = new LogicalChangeResultCallback(session);
      logicalChangeCallbacks.put(id, future);
      return future;
    } finally {
      rejoinCleanupLock.readLock().unlock();
    }
  }

  class LogicalChangeResultCallback {
    private LogicalChangeResult result;
    private final int           sessionForLogicalChange;

    public LogicalChangeResultCallback(int session) {
      this.sessionForLogicalChange = session;
    }

    public synchronized void handleResult(LogicalChangeResult resultParam) {
      this.result = resultParam;
      notifyAll();
    }

    public synchronized boolean getResult() throws PlatformRejoinException, AbortedOperationException {
      boolean interrupted = false;
      try {
        while (result == null) {
          if (this.sessionForLogicalChange != ClientTransactionManagerImpl.this.session) {
            // rejoin has happened
            throw new PlatformRejoinException();
          }
          try {
            wait(1000);
          } catch (InterruptedException e) {
            interrupted = true;
            if (ClientTransactionManagerImpl.this.abortableOperationManager.isAborted()) { throw new AbortedOperationException(); }
          }
        }
        return result.isSuccess();
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public synchronized void cleanup() {
      notifyAll();
    }

  }

  Map<LogicalChangeID, LogicalChangeResultCallback> getLogicalChangeCallbacks() {
    return logicalChangeCallbacks;
  }
}
