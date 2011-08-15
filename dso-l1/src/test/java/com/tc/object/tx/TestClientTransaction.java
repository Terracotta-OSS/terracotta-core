/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
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

  public Map getChangeBuffers() {
    return changeBuffers;
  }

  public Map getNewRoots() {
    return newRoots;
  }

  public LockID getLockID() {
    return lockID;
  }

  public List getAllLockIDs() {
    return new ArrayList(allLockIDs);
  }

  public TransactionID getTransactionID() {
    return txID;
  }

  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID rootID) {
    throw new ImplementMe();
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName) {
    throw new ImplementMe();
  }

  public boolean hasChangesOrNotifies() {
    return this.hasChangesOrNotifies;
  }

  public boolean isNull() {
    throw new ImplementMe();
  }

  public void addNotify(Notify notify) {
    this.notifies.add(notify);
  }

  public void setSequenceID(SequenceID sequenceID) {
    this.sequenceID = sequenceID;
  }

  public void setTransactionID(TransactionID tid) {
    this.txID = tid;
  }

  public SequenceID getSequenceID() {
    return this.sequenceID;
  }

  public boolean isConcurrent() {
    return false;
  }

  public void setTransactionContext(TransactionContext transactionContext) {
    throw new ImplementMe();
  }

  public void setAlreadyCommitted() {
    throw new ImplementMe();
  }

  public boolean hasChanges() {
    throw new ImplementMe();
  }

  public int getNotifiesCount() {
    return notifies.size();
  }

  public void arrayChanged(TCObject source, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  public Collection getReferencesOfObjectsInTxn() {
    return Collections.EMPTY_LIST;
  }

  public void addDmiDescriptor(DmiDescriptor dd) {
    throw new ImplementMe();
  }

  public List getDmiDescriptors() {
    return Collections.EMPTY_LIST;
  }

  public List getMetaDataDescriptors() {
    return Collections.EMPTY_LIST;
  }

  public void addMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    throw new ImplementMe();
  }

  public List getNotifies() {
    return notifies;
  }

  public TxnType getEffectiveType() {
    throw new ImplementMe();
  }

  public TxnType getLockType() {
    return txnType;
  }

  public void addTransactionCompleteListener(TransactionCompleteListener l) {
    txnListener.add(l);
  }

  public List getTransactionCompleteListeners() {
    return txnListener;
  }

}
