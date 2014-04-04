/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TestClientTransaction implements ClientTransaction {

  public TransactionID txID                 = TransactionID.NULL_ID;
  public LockID        lockID;
  public TxnType       txnType;
  public boolean       hasChangesOrNotifies = true;
  public Collection    allLockIDs           = new HashSet();
  private SequenceID   sequenceID;
  public Map           newRoots             = new HashMap();
  public Map           changeBuffers        = new HashMap();
  private final List   notifies             = new ArrayList();
  private final List   txnListener          = new ArrayList();

  public TestClientTransaction() {
    super();
  }

  @Override
  public Map getChangeBuffers() {
    return changeBuffers;
  }

  @Override
  public Map getNewRoots() {
    return newRoots;
  }

  @Override
  public LockID getLockID() {
    return lockID;
  }

  @Override
  public List getAllLockIDs() {
    return new ArrayList(allLockIDs);
  }

  @Override
  public TransactionID getTransactionID() {
    return txID;
  }

  @Override
  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  @Override
  public void createRoot(String name, ObjectID rootID) {
    throw new ImplementMe();
  }

  @Override
  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  @Override
  public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName,
 LogicalChangeID id) {
    throw new ImplementMe();
  }

  @Override
  public boolean hasChangesOrNotifies() {
    return this.hasChangesOrNotifies;
  }

  @Override
  public boolean isNull() {
    throw new ImplementMe();
  }

  @Override
  public void addNotify(Notify notify) {
    this.notifies.add(notify);
  }

  @Override
  public void setSequenceID(SequenceID sequenceID) {
    this.sequenceID = sequenceID;
  }

  @Override
  public void setTransactionID(TransactionID tid) {
    this.txID = tid;
  }

  @Override
  public SequenceID getSequenceID() {
    return this.sequenceID;
  }

  @Override
  public boolean isConcurrent() {
    return false;
  }

  @Override
  public void setTransactionContext(TransactionContext transactionContext) {
    throw new ImplementMe();
  }

  @Override
  public void setAlreadyCommitted() {
    throw new ImplementMe();
  }

  @Override
  public boolean hasChanges() {
    throw new ImplementMe();
  }

  @Override
  public int getNotifiesCount() {
    return notifies.size();
  }

  @Override
  public void arrayChanged(TCObject source, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  @Override
  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  @Override
  public Collection getReferencesOfObjectsInTxn() {
    return Collections.EMPTY_LIST;
  }

  public List getMetaDataDescriptors() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    throw new ImplementMe();
  }

  @Override
  public List getNotifies() {
    return notifies;
  }

  @Override
  public TxnType getEffectiveType() {
    throw new ImplementMe();
  }

  @Override
  public TxnType getLockType() {
    return txnType;
  }

  @Override
  public void addTransactionCompleteListener(TransactionCompleteListener l) {
    txnListener.add(l);
  }

  @Override
  public List getTransactionCompleteListeners() {
    return txnListener;
  }

  @Override
  public boolean isAtomic() {
    return false;
  }

  @Override
  public void setAtomic(boolean atomic) {
    //
  }

  @Override
  public void addOnCommitCallable(OnCommitCallable callable) {
    //
  }

  @Override
  public List<OnCommitCallable> getOnCommitCallables() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public int getSession() {
    return 0;
  }

}
