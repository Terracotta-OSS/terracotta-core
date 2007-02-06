/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.lockmanager.api.LockID;
import com.tc.util.SequenceID;

/**
 * Base class for client transaction implementations
 */
abstract class AbstractClientTransaction implements ClientTransaction {

  private SequenceID              seqID = SequenceID.NULL_ID;
  private final TransactionID     txID;
  private TransactionContext      transactionContext;
  private boolean                 alreadyCommittedFlag;
  private final ChannelIDProvider cidProvider;

  AbstractClientTransaction(TransactionID txID, ChannelIDProvider cidProvider) {
    this.txID = txID;
    this.cidProvider = cidProvider;
  }

  public synchronized void setSequenceID(SequenceID sequenceID) {
    if (!seqID.isNull()) throw new AssertionError("Attempt to set sequence id more than once.");
    if (sequenceID == null || sequenceID.isNull()) throw new AssertionError("Attempt to set sequence id to null: "
                                                                            + sequenceID);
    this.seqID = sequenceID;
  }

  public synchronized SequenceID getSequenceID() {
    return this.seqID;
  }

  public void setTransactionContext(TransactionContext transactionContext) {
    this.transactionContext = transactionContext;
  }

  public TxnType getTransactionType() {
    return transactionContext.getType();
  }

  public LockID[] getAllLockIDs() {
    return transactionContext.getAllLockIDs();
  }

  public TransactionID getTransactionID() {
    return txID;
  }

  public LockID getLockID() {
    return transactionContext.getLockID();
  }

  public final void createObject(TCObject source) {
    if (transactionContext.getType() == TxnType.READ_ONLY) { throw new AssertionError(
                                                                                      source.getClass().getName()
                                                                                          + " was already checked to have write access!"); }
    alreadyCommittedCheck();
    basicCreate(source);
  }

  public final void createRoot(String name, ObjectID rootID) {
    if (transactionContext.getType() == TxnType.READ_ONLY) { throw new AssertionError(
                                                                                      name
                                                                                          + " was already checked to have write access!"); }
    alreadyCommittedCheck();
    basicCreateRoot(name, rootID);
  }

  public final void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (transactionContext.getType() == TxnType.READ_ONLY) {
      throwReadOnlyException("Failed To Modify Field:  " + fieldname + " in " + classname);
    }
    alreadyCommittedCheck();
    basicFieldChanged(source, classname, fieldname, newValue, index);
  }

  public final void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    if (transactionContext.getType() == TxnType.READ_ONLY) {
      throwReadOnlyException("Failed To Change Value in:  " + newValue.getClass().getName());
    }
    alreadyCommittedCheck();
    basicLiteralValueChanged(source, newValue, oldValue);
  }

  public final void arrayChanged(TCObject source, int startPos, Object array, int length) {
    if (transactionContext.getType() == TxnType.READ_ONLY) {
      throwReadOnlyException("Failed To Modify Array: " + array.getClass().getName());
    }
    alreadyCommittedCheck();
    basicArrayChanged(source, startPos, array, length);
  }

  public final void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
    if (transactionContext.getType() == TxnType.READ_ONLY) {
      throwReadOnlyException("Failed Method Call: " + methodName);
    }
    alreadyCommittedCheck();
    basicLogicalInvoke(source, method, parameters);
  }

  public boolean isNull() {
    return false;
  }

  protected void alreadyCommittedCheck() {
    if (alreadyCommittedFlag) { throw new AssertionError("Transaction " + txID + " already commited."); }
  }

  private void throwReadOnlyException(String details) {
    long vmId = ReadOnlyException.INVALID_VMID;
    if (cidProvider != null) {
      vmId = cidProvider.getChannelID().toLong();
    }
    ReadOnlyException roe = new ReadOnlyException(
                                                  "Current transaction with read-only access attempted to modify a shared object.  "
                                                      + "\nPlease alter the locks section of your Terracotta configuration so that the methods involved in this transaction have read/write access.",
                                                  Thread.currentThread().getName(), vmId, details);
    System.err.println(roe.getMessage());
    throw roe;
  }

  public void setAlreadyCommitted() {
    alreadyCommittedCheck();
    this.alreadyCommittedFlag = true;
  }

  abstract protected void basicCreate(TCObject object);

  abstract protected void basicCreateRoot(String name, ObjectID rootID);

  abstract protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue,
                                            int index);

  abstract protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue);

  abstract protected void basicArrayChanged(TCObject source, int startPos, Object array, int length);

  abstract protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters);

}
