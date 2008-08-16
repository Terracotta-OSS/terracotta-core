/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.logging.DumpHandler;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Threadlocal based transaction manager interface. Changes go through here to the transaction for the current thread.
 * 
 * @author steve
 */
public interface ClientTransactionManager extends DumpHandler, PrettyPrintable {

  /**
   * Begin a thread local transaction
   * 
   * @param lock Lock name
   * @param lockLevel Lock level
   * @return If begun
   */
  public boolean begin(String lock, int lockLevel, String lockObjectType, String contextInfo);

  /**
   * Try with wait() to begin a thread local transaction.
   * 
   * @param lock Lock name
   * @param timeout Specification of wait()
   * @param lockLevel Lock level
   * @return If begun
   */
  public boolean tryBegin(String lock, TimerSpec timeout, int lockLevel, String lockObjectType);

  /**
   * Commit a thread local current transaction
   * 
   * @param lockName Lock name
   * @throws UnlockedSharedObjectException If a shared object is being accessed from outside a shared transaction
   */
  public void commit(String lockName) throws UnlockedSharedObjectException;

  /**
   * When transactions come in from the L2 we use this method to apply them. We will have to get a bit fancier because
   * we can't apply any changes while we are in any transaction. The first version will not allow apply to happen while
   * ANY txn is in progress. This is probably not acceptable. We will probably figure something out with the lock
   * manager where we can acquire a read lock if a field is accessed in a transaction
   * 
   * @param txType Transaction type
   * @param lockIDs Locks involved in the transaction
   * @param objectChanges Collection of DNA indicating changes
   * @param lookupObjectIDs ObjectIDs
   * @param newRoots Map of new roots, Root name -> ObjectID
   */
  public void apply(TxnType txType, List lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots);

  /**
   * Add new managed object to current transaction
   * 
   * @param source TCObject
   */
  public void createObject(TCObject source);

  /**
   * Add new root to current transaction
   * 
   * @param name Root name
   * @param id Object identifier
   */
  public void createRoot(String name, ObjectID id);

  /**
   * Record change in literal value in current transaction
   * 
   * @param source TCObject for literal value
   * @param newValue New value
   * @param oldValue Old value
   */
  public void literalValueChanged(TCObject source, Object newValue, Object oldValue);

  /**
   * Record field change in current transaction
   * 
   * @param source TCObject for field
   * @param classname Class name
   * @param fieldname Field name
   * @param newValue New object
   * @param index Into array if field is an array
   */
  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);

  /**
   * Record a logical method invocation
   * 
   * @param source TCObject for object
   * @param method Method constant from SerializationUtil
   * @param methodName Method name
   * @param parameters Parameter values in call
   */
  public void logicalInvoke(TCObject source, int method, String methodName, Object[] parameters);

  /**
   * Record wait() call on object in current transaction
   * 
   * @param lockName Lock name
   * @param call wait() call information
   * @param object Object being locked
   * @throws UnlockedSharedObjectException If shared object accessed outside lock
   * @throws InterruptedException If thread interrupted
   */
  public void wait(String lockName, TimerSpec call, Object object) throws UnlockedSharedObjectException,
      InterruptedException;

  /**
   * Record notify() or notifyAll() call on object in current transaction
   * 
   * @param lockName Lock name
   * @param all True if notifyAll()
   * @param object Object notify called on
   * @throws UnlockedSharedObjectException If shared object accessed outside lock
   */
  public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException;

  /**
   * Record acknowledgment
   * 
   * @param sessionID Session identifier
   * @param requestID Transaction identifier
   */
  public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID);

  /**
   * Record batch acknowledgment
   * 
   * @param batchID Transaction batch identifier
   */
  public void receivedBatchAcknowledgement(TxnBatchID batchID);

  /**
   * Check whether current transaction has write access
   * 
   * @param context The object context
   * @throws com.tc.object.util.ReadOnlyException If in read-only transaction
   */
  public void checkWriteAccess(Object context);

  /**
   * Add reference to tco in current transaction
   * 
   * @param tco TCObject
   */
  public void addReference(TCObject tco);

  /**
   * Get channel provider for this txn manager
   * 
   * @return Provider
   */
  public ChannelIDProvider getChannelIDProvider();

  /**
   * Check whether the lock with this name in this thread is holding the lock at this level
   * 
   * @param lockName Lock name
   * @param lockLevel Lock level
   * @return True if this lock is held by this thread at lockLevel
   */
  public boolean isLocked(String lockName, int lockLevel);

  // /**
  // * Lock this lock at this level
  // * @param lockName Lock name
  // * @param lockLevel Lock level
  // */
  // public void lock(String lockName, int lockLevel);

  /**
   * Unlock the lock
   * 
   * @param Lock name
   */
  public void unlock(String lockName);

  /**
   * Get number of threads holding this lock
   * 
   * @param lockName Lock name
   * @param lockLevel Lock level
   * @return Count
   */
  public int localHeldCount(String lockName, int lockLevel);

  /**
   * Check whether the current thread holds this lock
   * 
   * @param lockName Lock name
   * @param lockLevel Lock level
   */
  public boolean isHeldByCurrentThread(String lockName, int lockLevel);

  /**
   * Get current queue length on this lock
   * 
   * @param lockName Lock name
   */
  public int queueLength(String lockName);

  /**
   * Get wait() length on this lock
   * 
   * @param lockName Lock name
   * @return Wait length
   */
  public int waitLength(String lockName);

  /**
   * Enable transaction logging
   */
  public void enableTransactionLogging();

  /**
   * Disable transaction logging
   */
  public void disableTransactionLogging();

  /**
   * Check whether transaction logging is disabled
   * 
   * @return True if disabled, false if enabled
   */
  public boolean isTransactionLoggingDisabled();

  /**
   * Record an array change in the current transaction
   * 
   * @param src The TCObject for the array
   * @param startPos Start index in the array
   * @param array The new partial array or value
   * @param length Partial array length
   */
  public void arrayChanged(TCObject src, int startPos, Object array, int length);

  /**
   * Add distributed method call descriptor to current transaction
   * 
   * @param d Descriptor
   */
  public void addDmiDescriptor(DmiDescriptor d);

  /**
   * Check if lockID is on top of the transaction stack
   * 
   * @param lockName
   */
  public boolean isLockOnTopStack(String lockName);

}
