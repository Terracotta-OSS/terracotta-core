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
import com.tc.objectserver.managedobject.ManagedObjectStateSerializer;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
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

  private final ObjectID                id;
  private final ArrayList<ObjectID>     references;
  public boolean                        isDirty;
  public boolean                        isNew;

  public TestManagedObject(final ObjectID id, final ArrayList<ObjectID> references) {
    this.id = id;
    this.references = references;
  }

  public TestManagedObject(final ObjectID id) {
    this(id, new ArrayList<ObjectID>());
  }

  public void setReference(final int index, final ObjectID id) {

    if (index < this.references.size()) {
      this.references.set(index, id);
    } else {
      this.references.add(index, id);
    }
  }

  @Override
  public ObjectID getID() {
    return this.id;
  }

  @Override
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

  @Override
  public void toDNA(final TCByteBufferOutputStream out, final ObjectStringSerializer serializer, final DNAType dnaType) {
    throw new ImplementMe();
  }

  @Override
  public ManagedObjectFacade createFacade(final int limit) {
    throw new ImplementMe();
  }

  @Override
  public boolean isDirty() {
    return this.isDirty;
  }

  @Override
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

  @Override
  public boolean isNew() {
    return this.isNew;
  }

  public void setTransientState(final ManagedObjectStateFactory stateFactory) {
    this.setTransientStateCalls.put(stateFactory);
  }

  @Override
  public ManagedObjectReference getReference() {
    return this;
  }

  boolean removeOnRelease;

  @Override
  public void setRemoveOnRelease(final boolean removeOnRelease) {
    this.removeOnRelease = removeOnRelease;
  }

  @Override
  public boolean isRemoveOnRelease() {
    return this.removeOnRelease;
  }

  boolean referenced = false;

  @Override
  public synchronized boolean markReference() {
    if (!this.referenced) {
      this.referenced = true;
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean unmarkReference() {
    if (this.referenced) {
      this.referenced = false;
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean isReferenced() {
    return this.referenced;
  }

  @Override
  public ManagedObject getObject() {
    return this;
  }

  @Override
  public ObjectID getObjectID() {
    return getID();
  }

  @Override
  public ManagedObjectState getManagedObjectState() {
    return new NullManagedObjectState();
  }

  @Override
  public String toString() {
    return "TestManagedObject[" + this.id + "]";
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  @Override
  public void apply(final DNA dna, final TransactionID txnID, final ApplyTransactionInfo includeIDs,
                    final ObjectInstanceMonitor instanceMonitor, final boolean ignoreIfOlderDNA) throws DNAException {
    // TODO: do i need to implement this?
  }

  @Override
  public long getVersion() {
    return 0;
  }

  @Override
  public void setIsNew(final boolean newFlag) {
    this.isNew = newFlag;
  }

  @Override
  public void serializeTo(final ObjectOutput out, final ManagedObjectStateSerializer stateSerializer) throws IOException {
    out.writeLong(getVersion());
    out.writeLong(getObjectID().toLong());
    stateSerializer.serializeTo(getManagedObjectState(), out);
  }

  private class NullManagedObjectState extends AbstractManagedObjectState {
    private static final byte type = 0;

    @Override
    protected boolean basicEquals(final AbstractManagedObjectState o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType dnaType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getClassName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set getObjectReferences() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte getType() {
      return type;
    }

    @Override
    public void writeTo(final ObjectOutput o) {
      // Nothing to write, it's null.
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
