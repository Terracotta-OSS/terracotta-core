/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dna.api.LogicalChangeID;
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

  private TransactionID          txID   = TransactionID.NULL_ID;
  private SequenceID             seqID  = SequenceID.NULL_ID;
  private TransactionContext     transactionContext;
  private boolean                alreadyCommittedFlag;
  private List                   txnCompleteListener;
  private boolean                atomic = false;
  private List<OnCommitCallable> onCommitCallableQueue;

  @Override
  public void setSequenceID(SequenceID sequenceID) {
    if (!this.seqID.isNull()) {
      // Formatter
      throw new AssertionError("Attempt to set sequence id more than once : " + this.seqID + " : " + sequenceID);
    }

    if (sequenceID == null || sequenceID.isNull()) { throw new AssertionError("Attempt to set sequence id to null: "
                                                                              + sequenceID); }
    this.seqID = sequenceID;
  }

  @Override
  public void setTransactionID(TransactionID txnID) {
    if (!this.txID.isNull()) {
      // Formatter
      throw new AssertionError("Attempt to set Txn id more than once : " + this.txID + " : " + txnID);
    }
    if (txnID == null || txnID.isNull()) { throw new AssertionError("Attempt to set Transaction id to null: " + txnID); }
    this.txID = txnID;
  }

  @Override
  public SequenceID getSequenceID() {
    Assert.assertFalse(this.seqID.isNull());
    return this.seqID;
  }

  @Override
  public void setTransactionContext(TransactionContext transactionContext) {
    this.transactionContext = transactionContext;
  }

  @Override
  public TxnType getLockType() {
    return this.transactionContext.getLockType();
  }

  @Override
  public TxnType getEffectiveType() {
    return this.transactionContext.getEffectiveType();
  }

  @Override
  public List getAllLockIDs() {
    return this.transactionContext.getAllLockIDs();
  }

  /**
   * @return the transaction id for this transaction, null id if the transaction is not yet committed.
   */
  @Override
  public TransactionID getTransactionID() {
    return this.txID;
  }

  @Override
  public LockID getLockID() {
    return this.transactionContext.getLockID();
  }

  @Override
  public final void createObject(TCObject source) {
    alreadyCommittedCheck();
    basicCreate(source);
  }

  @Override
  public final void createRoot(String name, ObjectID rootID) {
    alreadyCommittedCheck();
    basicCreateRoot(name, rootID);
  }

  @Override
  public final void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (source.getTCClass().isEnum()) { throw new AssertionError("fieldChanged() on an enum type "
                                                                 + source.getTCClass().getPeerClass().getName()); }

    alreadyCommittedCheck();
    basicFieldChanged(source, classname, fieldname, newValue, index);
  }

  @Override
  public final void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    alreadyCommittedCheck();
    basicLiteralValueChanged(source, newValue, oldValue);
  }

  @Override
  public final void arrayChanged(TCObject source, int startPos, Object array, int length) {
    alreadyCommittedCheck();
    basicArrayChanged(source, startPos, array, length);
  }

  @Override
  public final void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName,
                                  LogicalChangeID id) {
    alreadyCommittedCheck();
    basicLogicalInvoke(source, method, parameters, id);
  }

  @Override
  public final void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    alreadyCommittedCheck();
    basicAddMetaDataDescriptor(tco, md);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  protected void alreadyCommittedCheck() {
    if (this.alreadyCommittedFlag) { throw new AssertionError("Transaction " + this.txID + " already commited."); }
  }

  @Override
  public void setAlreadyCommitted() {
    alreadyCommittedCheck();
    this.alreadyCommittedFlag = true;
  }

  /**
   * Adds a Transaction Complete Listener which will be called when the Transaction is complete.
   */
  @Override
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
  @Override
  public List getTransactionCompleteListeners() {
    return (txnCompleteListener == null ? Collections.EMPTY_LIST : txnCompleteListener);
  }

  @Override
  public boolean isAtomic() {
    return atomic;
  }

  @Override
  public void setAtomic(boolean atomic) {
    this.atomic = atomic;
  }

  @Override
  public void addOnCommitCallable(OnCommitCallable callable) {
    if (onCommitCallableQueue == null) {
      onCommitCallableQueue = new ArrayList<OnCommitCallable>();
    }
    onCommitCallableQueue.add(callable);
  }

  @Override
  public List<OnCommitCallable> getOnCommitCallables() {
    return (this.onCommitCallableQueue == null) ? (List<OnCommitCallable>) Collections.EMPTY_LIST
        : this.onCommitCallableQueue;
  }

  abstract protected void basicCreate(TCObject object);

  abstract protected void basicCreateRoot(String name, ObjectID rootID);

  abstract protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue,
                                            int index);

  abstract protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue);

  abstract protected void basicArrayChanged(TCObject source, int startPos, Object array, int length);

  abstract protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters, LogicalChangeID id);

  abstract protected void basicAddMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md);

}
