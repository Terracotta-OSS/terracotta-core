/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for client transaction implementations
 */
abstract class AbstractClientTransaction implements ClientTransaction {

  private TransactionID      txID  = TransactionID.NULL_ID;
  private SequenceID         seqID = SequenceID.NULL_ID;
  private TransactionContext transactionContext;
  private boolean            alreadyCommittedFlag;
  private List               txnCompleteListener;

  public void setSequenceID(SequenceID sequenceID) {
    if (!this.seqID.isNull()) {
      // Formatter
      throw new AssertionError("Attempt to set sequence id more than once : " + this.seqID + " : " + sequenceID);
    }

    if (sequenceID == null || sequenceID.isNull()) { throw new AssertionError("Attempt to set sequence id to null: "
                                                                              + sequenceID); }
    this.seqID = sequenceID;
  }

  public void setTransactionID(TransactionID txnID) {
    if (!this.txID.isNull()) {
      // Formatter
      throw new AssertionError("Attempt to set Txn id more than once : " + this.txID + " : " + txnID);
    }
    if (txnID == null || txnID.isNull()) { throw new AssertionError("Attempt to set Transaction id to null: " + txnID); }
    this.txID = txnID;
  }

  public SequenceID getSequenceID() {
    Assert.assertFalse(this.seqID.isNull());
    return this.seqID;
  }

  public void setTransactionContext(TransactionContext transactionContext) {
    this.transactionContext = transactionContext;
  }

  public TxnType getLockType() {
    return this.transactionContext.getLockType();
  }

  public TxnType getEffectiveType() {
    return this.transactionContext.getEffectiveType();
  }

  public List getAllLockIDs() {
    return this.transactionContext.getAllLockIDs();
  }

  /**
   * @return the transaction id for this transaction, null id if the transaction is not yet committed.
   */
  public TransactionID getTransactionID() {
    return this.txID;
  }

  public LockID getLockID() {
    return this.transactionContext.getLockID();
  }

  public final void createObject(TCObject source) {
    alreadyCommittedCheck();
    basicCreate(source);
  }

  public final void createRoot(String name, ObjectID rootID) {
    alreadyCommittedCheck();
    basicCreateRoot(name, rootID);
  }

  public final void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (source.getTCClass().isEnum()) { throw new AssertionError("fieldChanged() on an enum type "
                                                                 + source.getTCClass().getPeerClass().getName()); }

    alreadyCommittedCheck();
    basicFieldChanged(source, classname, fieldname, newValue, index);
  }

  public final void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    alreadyCommittedCheck();
    basicLiteralValueChanged(source, newValue, oldValue);
  }

  public final void arrayChanged(TCObject source, int startPos, Object array, int length) {
    alreadyCommittedCheck();
    basicArrayChanged(source, startPos, array, length);
  }

  public final void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
    alreadyCommittedCheck();
    basicLogicalInvoke(source, method, parameters);
  }

  public final void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    alreadyCommittedCheck();
    basicAddMetaDataDescriptor(tco, md);
  }

  public boolean isNull() {
    return false;
  }

  protected void alreadyCommittedCheck() {
    if (this.alreadyCommittedFlag) { throw new AssertionError("Transaction " + this.txID + " already commited."); }
  }

  public void setAlreadyCommitted() {
    alreadyCommittedCheck();
    this.alreadyCommittedFlag = true;
  }

  /**
   * Adds a Transaction Complete Listener which will be called when the Transaction is complete.
   */
  public void addTransactionCompleteListener(TransactionCompleteListener l) {
    if (txnCompleteListener == null) {
      txnCompleteListener = new ArrayList(5);
    }
    txnCompleteListener.add(l);
  }

  /**
   * Returns a list of Transaction Complete Listeners that should be called when the Transaction is complete.
   * 
   * @return List of TransactionCompleteListeners
   */
  public List getTransactionCompleteListeners() {
    return (txnCompleteListener == null ? Collections.EMPTY_LIST : txnCompleteListener);
  }

  abstract protected void basicCreate(TCObject object);

  abstract protected void basicCreateRoot(String name, ObjectID rootID);

  abstract protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue,
                                            int index);

  abstract protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue);

  abstract protected void basicArrayChanged(TCObject source, int startPos, Object array, int length);

  abstract protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters);

  abstract protected void basicAddMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md);

}
