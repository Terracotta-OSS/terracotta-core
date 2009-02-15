/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.TCClassNotFoundException;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.net.NodeID;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.appevent.NonPortableEventContextFactory;
import com.tc.object.appevent.ReadOnlyObjectEvent;
import com.tc.object.appevent.ReadOnlyObjectEventContext;
import com.tc.object.appevent.UnlockedSharedObjectEvent;
import com.tc.object.appevent.UnlockedSharedObjectEventContext;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.session.SessionID;
import com.tc.object.util.ReadOnlyException;
import com.tc.text.Banner;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.Util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ClientTransactionManagerImpl implements ClientTransactionManager {
  private static final TCLogger                logger          = TCLogging
                                                                   .getLogger(ClientTransactionManagerImpl.class);

  private final ThreadLocal                    transaction     = new ThreadLocal() {
                                                                 @Override
                                                                 protected Object initialValue() {
                                                                   return new ThreadTransactionContext();
                                                                 }
                                                               };

  // We need to remove initialValue() here because read auto locking now calls Manager.isDsoMonitored() which will
  // checks if isTransactionLogging is disabled. If it runs in the context of class loading, it will try to load
  // the class ThreadTransactionContext and thus throws a LinkageError.
  private final ThreadLocal                    txnLogging      = new ThreadLocal();

  private final ClientTransactionFactory       txFactory;
  private final RemoteTransactionManager       remoteTxManager;
  private final ClientObjectManager            objectManager;
  private final ThreadLockManager              lockManager;
  private final NonPortableEventContextFactory appEventContextFactory;
  private final LiteralValues                  literalValues   = new LiteralValues();

  private final WaitListener                   waitListener    = new WaitListener() {
                                                                 public void handleWaitEvent() {
                                                                   return;
                                                                 }
                                                               };

  private final ClientIDProvider               cidProvider;

  private final ClientTxMonitorMBean           txMonitor;

  private final boolean                        sendErrors      = System.getProperty("project.name") != null;

  private final ThreadLocal<List<LockID>>      locksWithoutTxn = new ThreadLocal<List<LockID>>() {
                                                                 @Override
                                                                 protected List<LockID> initialValue() {
                                                                   return new LinkedList<LockID>();
                                                                 }
                                                               };

  public ClientTransactionManagerImpl(final ClientIDProvider cidProvider, final ClientObjectManager objectManager,
                                      final ThreadLockManager lockManager, final ClientTransactionFactory txFactory,
                                      final RemoteTransactionManager remoteTxManager,
                                      final RuntimeLogger runtimeLogger, final ClientTxMonitorMBean txMonitor) {
    this.cidProvider = cidProvider;
    this.txFactory = txFactory;
    this.remoteTxManager = remoteTxManager;
    this.objectManager = objectManager;
    this.objectManager.setTransactionManager(this);
    this.lockManager = lockManager;
    this.txMonitor = txMonitor;
    this.appEventContextFactory = new NonPortableEventContextFactory(cidProvider);
  }

  public int queueLength(final String lockName) {
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    return this.lockManager.queueLength(lockID);
  }

  public int waitLength(final String lockName) {
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    return this.lockManager.waitLength(lockID);
  }

  public int localHeldCount(final String lockName, final int lockLevel) {
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    return this.lockManager.localHeldCount(lockID, lockLevel);
  }

  public boolean isHeldByCurrentThread(final String lockName, final int lockLevel) {
    if (isTransactionLoggingDisabled()) { return true; }
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    return this.lockManager.localHeldCount(lockID, lockLevel) > 0;
  }

  public boolean isLocked(final String lockName, final int lockLevel) {
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    return this.lockManager.isLocked(lockID, lockLevel);
  }

  public boolean tryBegin(final String lockName, final TimerSpec timeout, final int lockLevel,
                          final String lockObjectType) {
    logTryBegin0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || this.objectManager.isCreationInProgress()) { return true; }

    ClientTransaction currentTransaction = getTransactionOrNull();

    if ((currentTransaction != null) && lockLevel == LockLevel.CONCURRENT) {
      // make formatter sane
      throw new AssertionError("Can't acquire concurrent locks in a nested lock context.");
    }

    final LockID lockID = this.lockManager.lockIDFor(lockName);
    boolean isLocked = this.lockManager.tryLock(lockID, timeout, lockLevel, lockObjectType);
    if (!isLocked) { return isLocked; }

    pushTxContext(currentTransaction, lockID, lockLevel);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(this.peekContext());
    }

    return isLocked;
  }

  public boolean beginInterruptibly(final String lockName, final int lockLevel, final String lockObjectType,
                                    final String contextInfo) throws InterruptedException {
    logBeginInterruptibly0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || this.objectManager.isCreationInProgress()) { return true; }

    ClientTransaction currentTransaction = getTransactionOrNull();

    final LockID lockID = this.lockManager.lockIDFor(lockName);

    pushTxContext(currentTransaction, lockID, lockLevel);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(this.peekContext());
    }

    try {
      this.lockManager.lockInterruptibly(lockID, lockLevel, lockObjectType, contextInfo);
    } catch (TCLockUpgradeNotSupportedError e) {
      popTransaction(lockID);
      if (peekContext() != null) {
        currentTransaction.setTransactionContext(peekContext());
        setTransaction(currentTransaction);
      }
      throw e;
    } catch (InterruptedException e) {
      popTransaction(lockID);
      if (peekContext() != null) {
        currentTransaction.setTransactionContext(peekContext());
        setTransaction(currentTransaction);
      }
      throw e;
    }

    return true;
  }

  public boolean beginLockWithoutTxn(String lockName, int lockLevel, String lockObjectType, String contextInfo) {
    logBegin0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || this.objectManager.isCreationInProgress()) { return false; }

    final LockID lockID = this.lockManager.lockIDFor(lockName);
    this.lockManager.lock(lockID, lockLevel, lockObjectType, contextInfo);
    this.locksWithoutTxn.get().add(lockID);
    return true;
  }

  public boolean begin(final String lockName, final int lockLevel, final String lockObjectType, final String contextInfo) {
    logBegin0(lockName, lockLevel);

    if (isTransactionLoggingDisabled() || this.objectManager.isCreationInProgress()) { return false; }

    ClientTransaction currentTransaction = getTransactionOrNull();

    final LockID lockID = this.lockManager.lockIDFor(lockName);

    pushTxContext(currentTransaction, lockID, lockLevel);

    if (currentTransaction == null) {
      createTxAndInitContext();
    } else {
      currentTransaction.setTransactionContext(this.peekContext());
    }

    try {
      this.lockManager.lock(lockID, lockLevel, lockObjectType, contextInfo);
      return true;
    } catch (TCLockUpgradeNotSupportedError e) {
      popTransaction(lockID);
      if (peekContext() != null) {
        currentTransaction.setTransactionContext(peekContext());
        setTransaction(currentTransaction);
      }
      throw e;
    }
  }

  private TxnType getTxnTypeFromLockLevel(final int lockLevel) {
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

  public void wait(final String lockName, final TimerSpec call, final Object object)
      throws UnlockedSharedObjectException, InterruptedException {
    final ClientTransaction topTxn = getTransactionOrNull();

    if (topTxn == null) { throw new IllegalMonitorStateException(getIllegalMonitorStateExceptionMessage()); }

    LockID lockID = this.lockManager.lockIDFor(lockName);

    if (!this.lockManager.isLocked(lockID, LockLevel.WRITE)) { throw new IllegalMonitorStateException(
                                                                                                      getIllegalMonitorStateExceptionMessage()); }

    commit(lockID, topTxn, true);

    try {
      this.lockManager.wait(lockID, call, object, this.waitListener);
    } finally {
      createTxAndInitContext();
    }
  }

  public void notify(final String lockName, final boolean all, final Object object)
      throws UnlockedSharedObjectException {
    final ClientTransaction currentTxn = getTransactionOrNull();

    if (currentTxn == null) { throw new IllegalMonitorStateException(getIllegalMonitorStateExceptionMessage()); }

    LockID lockID = this.lockManager.lockIDFor(lockName);

    if (!this.lockManager.isLocked(lockID, LockLevel.WRITE)) { throw new IllegalMonitorStateException(
                                                                                                      getIllegalMonitorStateExceptionMessage()); }

    currentTxn.addNotify(this.lockManager.notify(lockID, all));
  }

  private String getIllegalMonitorStateExceptionMessage() {
    StringBuffer errorMsg = new StringBuffer("An IllegalStateMonitor is usually caused by one of the following:");
    errorMsg.append("\n");
    errorMsg.append("1) No synchronization");
    errorMsg.append("\n");
    errorMsg.append("2) The object synchronized is not the same as the object waited/notified");
    errorMsg.append("\n");
    errorMsg
        .append("3) The object being waited/notified on is a Terracotta distributed object, but no Terracotta auto-lock has been specified.");
    errorMsg.append("\n\n");
    errorMsg.append("For more information on this issue, please visit our Troubleshooting Guide at:");
    errorMsg.append("\n");
    errorMsg.append("http://terracotta.org/kit/troubleshooting");

    return Util.getFormattedMessage(errorMsg.toString());
  }

  private void logTryBegin0(final String lockID, final int type) {
    if (logger.isDebugEnabled()) {
      logger.debug("tryBegin(): lockID=" + (lockID == null ? "null" : lockID) + ", type = " + type);
    }
  }

  private void logBeginInterruptibly0(final String lockID, final int type) {
    if (logger.isDebugEnabled()) {
      logger.debug("beginInterruptibly(): lockID=" + (lockID == null ? "null" : lockID) + ", type = " + type);
    }
  }

  private void logBegin0(final String lockID, final int type) {
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

  private ClientTransaction getTransaction() throws UnlockedSharedObjectException {
    return getTransaction(null);
  }

  private ClientTransaction getTransaction(final Object context) throws UnlockedSharedObjectException {
    ClientTransaction tx = getTransactionOrNull();
    if (tx == null) {

      String type = context == null ? null : context.getClass().getName();
      String errorMsg = "Attempt to access a shared object outside the scope of a shared lock.\n"
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
                 + "For more information on how to solve this issue, see:\n" + "http://www.terracotta.org/usoe";

      throw new UnlockedSharedObjectException(errorMsg, Thread.currentThread().getName(), this.cidProvider
          .getClientID().toLong(), details);
    }
    return tx;
  }

  public void checkWriteAccess(final Object context) {
    if (isTransactionLoggingDisabled()) { return; }

    // First check if we have any TXN context at all (else exception thrown)
    ClientTransaction txn;

    try {
      txn = getTransaction(context);
    } catch (UnlockedSharedObjectException usoe) {
      if (this.sendErrors) {
        UnlockedSharedObjectEventContext eventContext = this.appEventContextFactory
            .createUnlockedSharedObjectEventContext(context, usoe);
        this.objectManager.sendApplicationEvent(context, new UnlockedSharedObjectEvent(eventContext));
      }

      throw usoe;
    }

    // make sure we're not in a read-only transaction
    if (txn.isEffectivelyReadOnly()) {
      ReadOnlyException roe = makeReadOnlyException(null);

      if (this.sendErrors) {
        ReadOnlyObjectEventContext eventContext = this.appEventContextFactory.createReadOnlyObjectEventContext(context,
                                                                                                               roe);
        this.objectManager.sendApplicationEvent(context, new ReadOnlyObjectEvent(eventContext));
      }

      throw roe;
    }
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
  public void commit(final String lockName) throws UnlockedSharedObjectException {
    logCommit0();
    if (isTransactionLoggingDisabled() || this.objectManager.isCreationInProgress()) { return; }

    LockID lockID = this.lockManager.lockIDFor(lockName);
    // if the current thread holds the lock and the lock is NOT associated with any transactions, just unlock and return
    if (this.locksWithoutTxn.get().contains(lockID)) {
      this.lockManager.unlock(lockID);
      this.locksWithoutTxn.get().remove(lockID);
      return;
    }

    ClientTransaction tx = getTransaction();
    if (lockID == null || lockID.isNull()) {
      lockID = tx.getLockID();
    }
    boolean hasCommitted = commit(lockID, tx, false);

    popTransaction(this.lockManager.lockIDFor(lockName));

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
    ClientTransaction ctx = this.txFactory.newInstance();
    ctx.setTransactionContext(peekContext());
    setTransaction(ctx);
  }

  private ClientTransaction popTransaction() {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.popCurrentTransaction();
  }

  private ClientTransaction popTransaction(final LockID lockID) {
    if (lockID == null || lockID.isNull()) { return popTransaction(); }
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.popCurrentTransaction(lockID);
  }

  private TransactionContext peekContext(final LockID lockID) {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.peekContext(lockID);
  }

  private TransactionContext peekContext() {
    ThreadTransactionContext ttc = getThreadTransactionContext();
    return ttc.peekContext();
  }

  public boolean isLockOnTopStack(final String lockName) {
    final LockID lockID = this.lockManager.lockIDFor(lockName);
    TransactionContext tc = peekContext();
    if (tc == null) { return false; }
    return (tc.getLockID().equals(lockID));
  }

  private void pushTxContext(final ClientTransaction currentTransaction, final LockID lockID, final int lockLevel) {
    final TxnType lockTxnType = getTxnTypeFromLockLevel(lockLevel);
    final TxnType effectiveTxnType;
    if (currentTransaction != null && TxnType.READ_ONLY == lockTxnType
        && TxnType.NORMAL == currentTransaction.getEffectiveType()) {
      effectiveTxnType = currentTransaction.getEffectiveType();
    } else {
      effectiveTxnType = lockTxnType;
    }

    ThreadTransactionContext ttc = getThreadTransactionContext();
    ttc.pushContext(lockID, lockTxnType, effectiveTxnType);
  }

  private void logCommit0() {
    if (logger.isDebugEnabled()) {
      logger.debug("commit()");
    }
  }

  private boolean commit(final LockID lockID, final ClientTransaction currentTransaction, final boolean isWaitContext) {
    try {
      return commitInternal(lockID, currentTransaction, isWaitContext);
    } catch (Throwable t) {
      this.remoteTxManager.stopProcessing();
      Banner.errorBanner("Terracotta client shutting down due to error " + t);
      logger.error(t);
      if (t instanceof Error) { throw (Error) t; }
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      throw new RuntimeException(t);
    }
  }

  private boolean commitInternal(final LockID lockID, final ClientTransaction currentTransaction,
                                 final boolean isWaitContext) {
    Assert.assertNotNull("transaction", currentTransaction);

    try {
      disableTransactionLogging();

      // If the current transactionContext is READ_ONLY, there is no need to commit.
      TransactionContext tc = peekContext(lockID);
      if (TxnType.READ_ONLY == tc.getLockType()) {
        this.txMonitor.committedReadTransaction();
        return false;
      }

      boolean hasPendingCreateObjects = this.objectManager.hasPendingCreateObjects();
      if (hasPendingCreateObjects) {
        this.objectManager.addPendingCreateObjectsToTransaction();
      }

      currentTransaction.setAlreadyCommitted();

      if (currentTransaction.hasChangesOrNotifies() || hasPendingCreateObjects) {
        if (this.txMonitor.isEnabled()) {
          currentTransaction.updateMBean(this.txMonitor);
        }
        this.remoteTxManager.commit(currentTransaction);
      }
      return true;
    } finally {
      enableTransactionLogging();

      // always try to unlock even if we are throwing an exception
      if (!isWaitContext && !currentTransaction.isNull()) {
        if (lockID != null && !lockID.isNull()) {
          this.lockManager.unlock(lockID);
        } else {
          throw new AssertionError("Trying to unlock with lockID = null!");
        }
      }
    }
  }

  private void basicApply(final Collection objectChanges, final Map newRoots, final boolean force) throws DNAException {
    List l = new LinkedList();

    for (Iterator i = objectChanges.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      TCObject tcobj = null;
      Assert.assertTrue(dna.isDelta());

      try {
        tcobj = this.objectManager.lookup(dna.getObjectID());
      } catch (ClassNotFoundException cnfe) {
        logger.warn("Could not apply change because class not local: " + dna.getTypeName());
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
          logger.warn("Could not apply change because class not local: " + cnfe.getMessage());
          throw new TCClassNotFoundException(cnfe);
        }
      }
    }

    for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      String rootName = (String) entry.getKey();
      ObjectID newRootID = (ObjectID) entry.getValue();
      this.objectManager.replaceRootIDIfNecessary(rootName, newRootID);
    }
  }

  public void receivedAcknowledgement(final SessionID sessionID, final TransactionID transactionID, final NodeID nodeID) {
    this.remoteTxManager.receivedAcknowledgement(sessionID, transactionID, nodeID);
  }

  public void receivedBatchAcknowledgement(final TxnBatchID batchID, final NodeID nodeID) {
    this.remoteTxManager.receivedBatchAcknowledgement(batchID, nodeID);
  }

  public void apply(final TxnType txType, final List lockIDs, final Collection objectChanges, final Map newRoots) {
    try {
      disableTransactionLogging();
      basicApply(objectChanges, newRoots, false);
    } finally {
      enableTransactionLogging();
    }
  }

  public void literalValueChanged(final TCObject source, final Object newValue, final Object oldValue) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();
      ClientTransaction tx;

      try {
        tx = getTransaction(pojo);
      } catch (UnlockedSharedObjectException usoe) {
        if (this.sendErrors) {
          UnlockedSharedObjectEventContext eventContext = this.appEventContextFactory
              .createUnlockedSharedObjectEventContext(pojo, usoe);
          this.objectManager.sendApplicationEvent(pojo, new UnlockedSharedObjectEvent(eventContext));
        }

        throw usoe;
      }

      if (tx.isEffectivelyReadOnly()) {
        ReadOnlyException roe = makeReadOnlyException("Failed To Change Value in:  " + newValue.getClass().getName());

        if (this.sendErrors) {
          ReadOnlyObjectEventContext eventContext = this.appEventContextFactory.createReadOnlyObjectEventContext(pojo,
                                                                                                                 roe);
          this.objectManager.sendApplicationEvent(pojo, new ReadOnlyObjectEvent(eventContext));
        }

        throw roe;
      }

      tx.literalValueChanged(source, newValue, oldValue);

    } finally {
      enableTransactionLogging();
    }

  }

  public void fieldChanged(final TCObject source, final String classname, final String fieldname,
                           final Object newValue, final int index) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();

      ClientTransaction tx;
      try {
        tx = getTransaction(pojo);
      } catch (UnlockedSharedObjectException usoe) {
        if (this.sendErrors) {
          UnlockedSharedObjectEventContext eventContext = this.appEventContextFactory
              .createUnlockedSharedObjectEventContext(pojo, classname, fieldname, usoe);
          this.objectManager.sendApplicationEvent(pojo, new UnlockedSharedObjectEvent(eventContext));
        }

        throw usoe;
      }

      if (tx.isEffectivelyReadOnly()) {
        ReadOnlyException roe = makeReadOnlyException("Failed To Modify Field:  " + fieldname + " in " + classname);
        if (this.sendErrors) {
          ReadOnlyObjectEventContext eventContext = this.appEventContextFactory
              .createReadOnlyObjectEventContext(pojo, classname, fieldname, roe);
          this.objectManager.sendApplicationEvent(pojo, new ReadOnlyObjectEvent(eventContext));
        }
        throw roe;
      }

      logFieldChanged0(source, classname, fieldname, newValue, tx);

      if (newValue != null && this.literalValues.isLiteralInstance(newValue)) {
        tx.fieldChanged(source, classname, fieldname, newValue, index);
      } else {
        if (newValue != null) {
          this.objectManager.checkPortabilityOfField(newValue, fieldname, pojo);
        }

        TCObject tco = this.objectManager.lookupOrCreate(newValue);

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

  public void arrayChanged(final TCObject source, final int startPos, final Object array, final int length) {
    if (isTransactionLoggingDisabled()) { return; }
    try {
      disableTransactionLogging();
      Object pojo = source.getPeerObject();
      ClientTransaction tx;

      try {
        tx = getTransaction(pojo);
      } catch (UnlockedSharedObjectException usoe) {
        if (this.sendErrors) {
          UnlockedSharedObjectEventContext eventContext = this.appEventContextFactory
              .createUnlockedSharedObjectEventContext(pojo, usoe);
          this.objectManager.sendApplicationEvent(pojo, new UnlockedSharedObjectEvent(eventContext));
        }

        throw usoe;
      }

      if (tx.isEffectivelyReadOnly()) {
        ReadOnlyException roe = makeReadOnlyException("Failed To Modify Array: " + pojo.getClass().getName());

        if (this.sendErrors) {
          ReadOnlyObjectEventContext eventContext = this.appEventContextFactory.createReadOnlyObjectEventContext(pojo,
                                                                                                                 roe);
          this.objectManager.sendApplicationEvent(pojo, new ReadOnlyObjectEvent(eventContext));
        }
        throw roe;

      }

      if (!ClassUtils.isPrimitiveArray(array)) {
        Object[] objArray = (Object[]) array;
        for (int i = 0; i < length; i++) {

          Object element = objArray[i];
          if (!this.literalValues.isLiteralInstance(element)) {
            if (element != null) {
              this.objectManager.checkPortabilityOfField(element, String.valueOf(i), pojo);
            }

            TCObject tco = this.objectManager.lookupOrCreate(element);
            objArray[i] = tco.getObjectID();
            // record the reference in this transaction -- This is to solve the race condition of transactions
            // that reference objects newly "created" in other transactions that may not commit before us
            if (element != null) {
              tx.createObject(tco);
            }
          }
        }
      }

      tx.arrayChanged(source, startPos, array, length);

    } finally {
      enableTransactionLogging();
    }
  }

  private void logFieldChanged0(final TCObject source, final String classname, final String fieldname,
                                final Object newValue, final ClientTransaction tx) {
    if (logger.isDebugEnabled()) {
      logger.debug("fieldChanged(source=" + source + ", classname=" + classname + ", fieldname=" + fieldname
                   + ", newValue=" + newValue + ", tx=" + tx);
    }
  }

  public void logicalInvoke(final TCObject source, final int method, final String methodName, final Object[] parameters) {
    if (isTransactionLoggingDisabled()) { return; }

    try {
      disableTransactionLogging();

      Object pojo = source.getPeerObject();
      ClientTransaction tx;

      try {
        tx = getTransaction(pojo);
      } catch (UnlockedSharedObjectException usoe) {
        if (this.sendErrors) {
          UnlockedSharedObjectEventContext eventContext = this.appEventContextFactory
              .createUnlockedSharedObjectEventContext(pojo, usoe);
          pojo = this.objectManager.cloneAndInvokeLogicalOperation(pojo, methodName, parameters);
          this.objectManager.sendApplicationEvent(pojo, new UnlockedSharedObjectEvent(eventContext));
        }

        throw usoe;
      }

      if (tx.isEffectivelyReadOnly()) {
        ReadOnlyException roe = makeReadOnlyException("Failed Method Call: " + methodName);

        if (this.sendErrors) {
          ReadOnlyObjectEventContext eventContext = this.appEventContextFactory.createReadOnlyObjectEventContext(pojo,
                                                                                                                 roe);
          pojo = this.objectManager.cloneAndInvokeLogicalOperation(pojo, methodName, parameters);
          this.objectManager.sendApplicationEvent(pojo, new ReadOnlyObjectEvent(eventContext));
        }
        throw roe;
      }

      for (int i = 0; i < parameters.length; i++) {
        Object p = parameters[i];
        boolean isLiteral = this.literalValues.isLiteralInstance(p);
        if (!isLiteral) {
          if (p != null) {
            this.objectManager.checkPortabilityOfLogicalAction(parameters, i, methodName, pojo);
          }

          TCObject tco = this.objectManager.lookupOrCreate(p);
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

  private ReadOnlyException makeReadOnlyException(final String details) {
    long vmId = this.cidProvider.getClientID().toLong();

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

  public void createObject(final TCObject source) {
    getTransaction().createObject(source);
  }

  public void createRoot(final String name, final ObjectID rootID) {
    getTransaction().createRoot(name, rootID);
  }

  public ClientTransaction getCurrentTransaction() {
    return getTransactionOrNull();
  }

  public void addReference(final TCObject tco) {
    ClientTransaction txn = getTransactionOrNull();
    if (txn != null) {
      txn.createObject(tco);
    }
  }

  public ClientIDProvider getClientIDProvider() {
    return this.cidProvider;
  }

  public void disableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    if (txnStack == null) {
      txnStack = new ThreadTransactionLoggingStack();
      this.txnLogging.set(txnStack);
    }
    txnStack.increment();
  }

  public void enableTransactionLogging() {
    ThreadTransactionLoggingStack txnStack = (ThreadTransactionLoggingStack) this.txnLogging.get();
    Assert.assertNotNull(txnStack);
    final int size = txnStack.decrement();

    if (size < 0) { throw new AssertionError("size=" + size); }
  }

  public boolean isTransactionLoggingDisabled() {
    Object txnStack = this.txnLogging.get();
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

  public void addDmiDescriptor(final DmiDescriptor dd) {
    getTransaction().addDmiDescritor(dd);
  }

  public String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  public void dump(final Writer writer) {
    try {
      writer.write(dump());
      writer.flush();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void dumpToLogger() {
    logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {

    out.println(getClass().getName());
    return out;

  }

  private static final String READ_ONLY_TEXT = "Attempt to write to a shared object inside the scope of a lock declared as a"
                                               + "\nread lock. All writes to shared objects must be within the scope of one or"
                                               + "\nmore shared locks with write access defined in your Terracotta configuration."
                                               + "\n\nPlease alter the locks section of your Terracotta configuration so that this"
                                               + "\naccess is auto-locked or protected by a named lock with write access."
                                               + "\n\nFor more information on this issue, please visit our Troubleshooting Guide at:"
                                               + "\nhttp://terracotta.org/kit/troubleshooting ";

}
