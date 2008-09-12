/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Hangs on to a grouping of changes to be sent as a batch to the server. Changes are accumulated by the
 * ClientTransactionManager.
 *
 * @author steve
 */
public interface ClientTransaction {

  /**
   * Set the transaction context (type and related locks)
   *
   * @param transactionContext Context
   */
  public void setTransactionContext(TransactionContext transactionContext);

  /**
   * Get all change buffers associated with this transaction
   *
   * @return Map of ObjectID to TCChangeBuffer
   */
  public Map getChangeBuffers();

  /**
   * Get new roots in this transaction
   *
   * @return Map of Root name to ObjectID
   */
  public Map getNewRoots();

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
  public List getAllLockIDs();

  /**
   * Get transaction identifier
   *
   * @return Identifier
   */
  public TransactionID getTransactionID();

  /**
   * Record new object creation
   *
   * @param source TCObject for new object
   */
  public void createObject(TCObject source);

  /**
   * Record new root
   *
   * @param name Root name
   * @param rootID ObjectID for root value
   */
  public void createRoot(String name, ObjectID rootID);

  /**
   * Record field changed
   *
   * @param source TCObject for value
   * @param classname Class name
   * @param fieldname Field name
   * @param newValue New field value
   * @param index Index into array if array field
   */
  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);

  /**
   * Record literal value changed
   *
   * @param source TCObject for instance
   * @param newValue New value for instance
   * @param oldValue Old value for instance
   */
  public void literalValueChanged(TCObject source, Object newValue, Object oldValue);

  /**
   * Record array change
   *
   * @param source TCObject for instance
   * @param startPos Index into array
   * @param array Partial array or value
   * @param length Length of array
   */
  public void arrayChanged(TCObject source, int startPos, Object array, int length);

  /**
   * Record logical invocation
   *
   * @param source Source of invoke
   * @param method Method identifier
   * @param parameters Parameter values
   * @param methodName Method name
   */
  public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName);

  /**
   * Check whether transactoin has changes or notifications
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
   * Returns the transaction type that corresponds to the lock type that
   * initiated this transaction.
   * 
   * @return Type of transaction based on the lock
   * @see #getEffectiveType()
   */
  public TxnType getLockType();

  /**
   * Returns the effective transaction type.
   *
   * Read the docs of {@link TransactionContext#getEffectiveType()} for more details.
   * 
   * @return Type of transaction based on the context
   * @see #getLockType()
   */
  public TxnType getEffectiveType();
  
  /**
   * Evaluates the effective transaction type to see if it's read-only.
   * 
   * Read the docs of {@link TransactionContext#getEffectiveType()} for more details.
   * 
   * @return {@code true} when the effective transaction type is read-only, or
   * {@code false} when it's not
   * @see #getEffectiveType()
   */
  public boolean isEffectivelyReadOnly();

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
   * @return True if has changes in transaction
   */
  public boolean hasChanges();

  /**
   * @return Count of notify's in transaction
   */
  public int getNotifiesCount();

  /**
   * Update MBean with state
   *
   * @param txMBean MBean to update
   */
  public void updateMBean(ClientTxMonitorMBean txMBean);

  /**
   * Get all references of objects included in the transaction
   *
   * @return Collection of referenced objects
   */
  public Collection getReferencesOfObjectsInTxn();

  /**
   * Add a new DMI descriptor
   *
   * @param dd Descriptor
   */
  public void addDmiDescritor(DmiDescriptor dd);

  /**
   * Get all DmiDescriptors
   *
   * @return List of DmiDescriptors in transaction
   */
  public List getDmiDescriptors();

  /**
   * Get all Notify calls
   *
   * @return List of notify/notifyAll() calls in this transaction
   */
  public List getNotifies();

}
