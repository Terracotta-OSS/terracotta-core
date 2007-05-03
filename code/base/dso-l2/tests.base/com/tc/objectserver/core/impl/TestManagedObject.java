/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import gnu.trove.TLinkable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author steve
 */
public class TestManagedObject implements ManagedObject, ManagedObjectReference, Serializable {
  public final NoExceptionLinkedQueue setTransientStateCalls = new NoExceptionLinkedQueue();
  private ObjectID                    id;
  private ObjectID[]                  references;
  private boolean                     isDirty;

  public TestManagedObject(ObjectID id, ObjectID[] references) {
    this.id = id;
    this.references = references;
  }

  public TestManagedObject(ObjectID id) {
    this(id, new ObjectID[0]);
  }

  public void setReference(int index, ObjectID id) {
    this.references[index] = id;
  }

  public ObjectID getID() {
    return id;
  }

  public Set getObjectReferences() {
    return new HashSet(Arrays.asList(references));
  }

  public void apply(DNA dna, TransactionID txID, BackReferences includeIDs, ObjectInstanceMonitor imo) {
    // do nothing
  }

  public void commit() {
    return;
  }

  public void toDNA(TCByteBufferOutputStream out, ObjectStringSerializer serializer) {
    throw new ImplementMe();
  }

  public void setObjectStore(ManagedObjectStore store) {
    return;
  }

  public ManagedObjectFacade createFacade(int limit) {
    throw new ImplementMe();
  }

  public boolean isDirty() {
    return this.isDirty;
  }

  public void setIsDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  public void setReferences(ObjectID[] references) {
    this.references = references;
  }

  public boolean isNew;
  public boolean isNew() {
    return this.isNew;
  }

  public void setTransientState(ManagedObjectStateFactory stateFactory) {
    setTransientStateCalls.put(stateFactory);
  }

  public ManagedObjectReference getReference() {
    return this;
  }
  
  boolean processPendingOnRelease;
  public boolean getProcessPendingOnRelease() {
    return processPendingOnRelease;
  }

  public void setProcessPendingOnRelease(boolean b) {
    this.processPendingOnRelease = b;
  }

  boolean removeOnRelease;
  public void setRemoveOnRelease(boolean removeOnRelease) {
    this.removeOnRelease = removeOnRelease;
  }

  public boolean isRemoveOnRelease() {
    return removeOnRelease;
  }

  boolean referenced;
  public void markReference() {
    referenced = true;
  }

  public void unmarkReference() {
    referenced = false;
  }

  public boolean isReferenced() {
    return referenced;
  }
  
  public ManagedObject getObject() {
    return this;
  }

  public ObjectID getObjectID() {
    return getID();
  }

  public void markAccessed() {
    throw new ImplementMe();
  }

  public void clearAccessed() {
    throw new ImplementMe();
  }

  public boolean recentlyAccessed() {
    throw new ImplementMe();
  }

  public int accessCount(int accessed) {
    throw new ImplementMe();
  }

  TLinkable next;
  TLinkable previous;
  
  public TLinkable getNext() {
    return this.next;
  }

  public TLinkable getPrevious() {
    return this.previous;
  }

  public void setNext(TLinkable linkable) {
    this.next = linkable;
  }

  public void setPrevious(TLinkable linkable) {
    this.previous = linkable;
  }

  public ManagedObjectState getManagedObjectState() {
    throw new ImplementMe();
  }
  
  public String toString() {
    return "TestManagedObject["+id+"]";
  }

  public boolean canEvict() {
    return true;
  }

  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    return;
  }

  public void apply(DNA dna, TransactionID txnID, BackReferences includeIDs, ObjectInstanceMonitor instanceMonitor, boolean ignoreIfOlderDNA) throws DNAException {
    throw new ImplementMe();
  }

  public long getVersion() {
    throw new ImplementMe();
  }
}
