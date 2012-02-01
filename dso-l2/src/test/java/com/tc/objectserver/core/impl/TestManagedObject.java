/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.managedobject.AbstractManagedObjectState;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import gnu.trove.TLinkable;

import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author steve
 */
public class TestManagedObject implements ManagedObject, ManagedObjectReference, Serializable {
  public final NoExceptionLinkedQueue   setTransientStateCalls   = new NoExceptionLinkedQueue();

  public static final TestManagedObject NULL_TEST_MANAGED_OBJECT = new TestManagedObject(new ObjectID(-1));
  private final ObjectID                id;
  private final ArrayList<ObjectID>     references;
  public boolean                        isDirty;
  public boolean                        isNew;
  public boolean                        noReferences;
  TLinkable                             next                     = null;
  TLinkable                             previous                 = null;

  public TestManagedObject(final ObjectID id, final ArrayList<ObjectID> references) {
    this(id, references, false);
  }

  public TestManagedObject(final ObjectID id, final ArrayList<ObjectID> references, final boolean noReferences) {
    this.id = id;
    this.references = references;
    this.noReferences = noReferences;
  }

  public TestManagedObject(final ObjectID id) {
    this(id, new ArrayList<ObjectID>(), false);
  }

  public void setReference(final int index, final ObjectID id) {

    if (index < this.references.size()) {
      this.references.set(index, id);
    } else {
      this.references.add(index, id);
    }
  }

  public ObjectID getID() {
    return this.id;
  }

  public synchronized Set<ObjectID> getObjectReferences() {
    return new HashSet<ObjectID>(this.references);
  }

  public void apply(final DNA dna, final TransactionID txID, final ApplyTransactionInfo includeIDs,
                    final ObjectInstanceMonitor imo) {
    // do nothing
  }

  public void commit() {
    return;
  }

  public void toDNA(final TCByteBufferOutputStream out, final ObjectStringSerializer serializer, final DNAType dnaType) {
    throw new ImplementMe();
  }

  public void setObjectStore(final ManagedObjectStore store) {
    return;
  }

  public ManagedObjectFacade createFacade(final int limit) {
    throw new ImplementMe();
  }

  public boolean isDirty() {
    return this.isDirty;
  }

  public void setIsDirty(final boolean isDirty) {
    this.isDirty = isDirty;
  }

  public synchronized void addReferences(final Set<ObjectID> ids) {
    for (final ObjectID oid : ids) {
      this.references.add(oid);
    }
  }

  public synchronized void addReferences(final Set<ObjectID> ids, final ObjectManagerImpl[] objectManagers) {
    for (final ObjectID oid : ids) {
      this.references.add(oid);
      objectManagers[oid.getGroupID()].changed(null, null, oid);
    }
  }

  public synchronized void removeReferences(final Set<ObjectID> ids) {
    for (final ObjectID objectID : ids) {
      this.references.remove(objectID);
    }
  }

  public boolean isNew() {
    return this.isNew;
  }

  public boolean isNewInDB() {
    return false;
  }

  public void setTransientState(final ManagedObjectStateFactory stateFactory) {
    this.setTransientStateCalls.put(stateFactory);
  }

  public ManagedObjectReference getReference() {
    return this;
  }

  boolean removeOnRelease;

  public void setRemoveOnRelease(final boolean removeOnRelease) {
    this.removeOnRelease = removeOnRelease;
  }

  public boolean isRemoveOnRelease() {
    return this.removeOnRelease;
  }

  boolean referenced = false;

  public synchronized boolean markReference() {
    if (!this.referenced) {
      this.referenced = true;
      return true;
    }
    return false;
  }

  public synchronized boolean unmarkReference() {
    if (this.referenced) {
      this.referenced = false;
      return true;
    }
    return false;
  }

  public synchronized boolean isReferenced() {
    return this.referenced;
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

  public int accessCount(final int accessed) {
    throw new ImplementMe();
  }

  public TLinkable getNext() {
    return this.next;
  }

  public TLinkable getPrevious() {
    return this.previous;
  }

  public void setNext(final TLinkable linkable) {
    this.next = linkable;
  }

  public void setPrevious(final TLinkable linkable) {
    this.previous = linkable;
  }

  public ManagedObjectState getManagedObjectState() {
    return this.noReferences ? new NullNoReferencesManagedObjectState() : new NullManagedObjectState();
  }

  @Override
  public String toString() {
    return "TestManagedObject[" + this.id + "]";
  }

  public boolean canEvict() {
    return true;
  }

  public boolean isCacheManaged() {
    return true;
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final DNA dna, final TransactionID txnID, final ApplyTransactionInfo includeIDs,
                    final ObjectInstanceMonitor instanceMonitor, final boolean ignoreIfOlderDNA) throws DNAException {
    // TODO: do i need to implement this?
  }

  public long getVersion() {
    throw new ImplementMe();
  }

  public void setIsNew(final boolean newFlag) {
    this.isNew = newFlag;
  }

  private class NullNoReferencesManagedObjectState extends NullManagedObjectState {

    @Override
    public boolean hasNoReferences() {
      return true;
    }
  }

  private class NullManagedObjectState extends AbstractManagedObjectState {
    private static final byte type = 0;

    @Override
    protected boolean basicEquals(final AbstractManagedObjectState o) {
      throw new UnsupportedOperationException();
    }

    public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
      throw new UnsupportedOperationException();
    }

    public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs) {
      throw new UnsupportedOperationException();
    }

    public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
      throw new UnsupportedOperationException();
    }

    public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType dnaType) {
      throw new UnsupportedOperationException();
    }

    public String getClassName() {
      throw new UnsupportedOperationException();
    }

    public Set getObjectReferences() {
      throw new UnsupportedOperationException();
    }

    public byte getType() {
      return type;
    }

    public void writeTo(final ObjectOutput o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + type;
      return result;
    }

    private TestManagedObject getOuterType() {
      return TestManagedObject.this;
    }

  }

}
