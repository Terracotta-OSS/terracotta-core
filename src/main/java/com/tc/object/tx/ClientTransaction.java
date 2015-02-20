/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.util.SequenceID;

import java.util.List;
import java.util.Map;

/**
 * Hangs on to a grouping of changes to be sent as a batch to the server. Changes are accumulated by the
 * ClientTransactionManager.
 */
public interface ClientTransaction {

  /**
   * Set the transaction context (type and related locks)
   * 
   * @param transactionContext Context
   */
  public void setTransactionContext(TransactionContext transactionContext);

  /**
   * Get the change buffer associated with this transaction
   * 
   * @return changebuffer
   */
  public TCChangeBuffer getChangeBuffer();

  /**
   * Get initial lock identifier
   * 
   * @return Identifier
   */
  public LockID getLockID();

  /**
   * Get all locks associated with this transaction
   * 
   * @return All locks
   */
  public List<LockID> getAllLockIDs();

  /**
   * Indicate place in sequence of transactions
   * 
   * @param tid Identifier
   */
  public void setTransactionID(TransactionID tid);

  /**
   * Get transaction identifier
   * 
   * @return the transaction Identifier is set, else return TransactionID.NULL_ID
   */
  public TransactionID getTransactionID();

  /**
   * Record new object creation
   * 
   * @param source TCObject for new object
   */
  public void createObject(TCObject source);

  /**
   * Record logical invocation
   * 
   * @param source Source of invoke
   * @param method Method identifier
   * @param parameters Parameter values
   */
  public void logicalInvoke(TCObject source, LogicalOperation method, Object[] parameters, LogicalChangeID id);

  /**
   * Check whether transaction has changes or notifications
   * 
   * @return True if has changes or notifies
   */
  public boolean hasChangesOrNotifies();

  /**
   * Check whether this is a null transaction
   * 
   * @return True if null
   */
  public boolean isNull();

  /**
   * Returns the transaction type that corresponds to the lock type that initiated this transaction.
   * 
   * @return Type of transaction based on the lock
   * @see #getEffectiveType()
   */
  public TxnType getLockType();

  /**
   * Returns the effective transaction type. Read the docs of {@link TransactionContext#getEffectiveType()} for more
   * details.
   * 
   * @return Type of transaction based on the context
   * @see #getLockType()
   */
  public TxnType getEffectiveType();

  /**
   * Add a new Notify
   * 
   * @param notify Notify
   */
  public void addNotify(Notify notify);

  /**
   * Indicate place in sequence of transactions
   * 
   * @param sequenceID Identifier
   */
  public void setSequenceID(SequenceID sequenceID);

  /**
   * @return Sequence identifier in transaction stream
   */
  public SequenceID getSequenceID();

  /**
   * @return True if concurrent transaction
   */
  public boolean isConcurrent();

  /**
   * Set transaction as already committed
   */
  public void setAlreadyCommitted();

  /**
   * @return Count of notify's in transaction
   */
  public int getNotifiesCount();

  /**
   * Get all Notify calls
   * 
   * @return List of notify/notifyAll() calls in this transaction
   */
  public List<Notify> getNotifies();

  /**
   * Adds a Transaction Complete Listener which will be called when the Transaction is complete.
   */
  public void addTransactionCompleteListener(TransactionCompleteListener l);

  /**
   * Returns a list of Transaction Complete Listeners that should be called when the Transaction is complete.
   * 
   * @return List of TransactionCompleteListeners
   */
  public List<TransactionCompleteListener> getTransactionCompleteListeners();

  public boolean isAtomic();

  public void setAtomic(boolean atomic);

  public void addOnCommitCallable(OnCommitCallable callable);

  public List<OnCommitCallable> getOnCommitCallables();

  /**
   * returns the session in which this transaction was created
   */
  public int getSession();

}
