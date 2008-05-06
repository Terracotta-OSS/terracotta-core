/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.lockmanager.api.LockID;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.util.List;

/**
 * Base class for client transaction implementations
 */
abstract class AbstractClientTransaction implements ClientTransaction {

  private final TransactionID txID;
  private SequenceID          seqID = SequenceID.NULL_ID;
  private TransactionContext  transactionContext;
  private boolean             alreadyCommittedFlag;

  AbstractClientTransaction(TransactionID txID) {
    this.txID = txID;
  }

  public synchronized void setSequenceID(SequenceID sequenceID) {
    if (!seqID.isNull()) throw new AssertionError("Attempt to set sequence id more than once.");
    if (sequenceID == null || sequenceID.isNull()) throw new AssertionError("Attempt to set sequence id to null: "
                                                                            + sequenceID);
    this.seqID = sequenceID;
  }

  public synchronized SequenceID getSequenceID() {
    Assert.assertFalse(this.seqID.isNull());
    return this.seqID;
  }

  public void setTransactionContext(TransactionContext transactionContext) {
    this.transactionContext = transactionContext;
  }

  public TxnType getTransactionType() {
    return transactionContext.getType();
  }

  public List getAllLockIDs() {
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
    assertNotReadOnlyTxn();
    if (source.getTCClass().isEnum()) { throw new AssertionError("fieldChanged() on an enum type "
                                                                 + source.getTCClass().getPeerClass().getName()); }

    alreadyCommittedCheck();
    basicFieldChanged(source, classname, fieldname, newValue, index);
  }

  public final void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    assertNotReadOnlyTxn();
    alreadyCommittedCheck();
    basicLiteralValueChanged(source, newValue, oldValue);
  }

  public final void arrayChanged(TCObject source, int startPos, Object array, int length) {
    assertNotReadOnlyTxn();
    alreadyCommittedCheck();
    basicArrayChanged(source, startPos, array, length);
  }

  public final void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
    assertNotReadOnlyTxn();
    alreadyCommittedCheck();
    basicLogicalInvoke(source, method, parameters);
  }

  public boolean isNull() {
    return false;
  }

  protected void alreadyCommittedCheck() {
    if (alreadyCommittedFlag) { throw new AssertionError("Transaction " + txID + " already commited."); }
  }

  private void assertNotReadOnlyTxn() {
    if (transactionContext.getType() == TxnType.READ_ONLY) { throw new AssertionError("fail to perform read only check"); }
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
