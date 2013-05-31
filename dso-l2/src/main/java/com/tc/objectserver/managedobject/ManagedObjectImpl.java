/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.ObjectDNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.TCCollections;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

/**
 * Responsible for maintaining the state of a shared object. Used for broadcasting new instances of an object as well as
 * having changes applied to it and keeping track of references for garbage collection. If you add fields to this object
 * that need to be serialized make sure you add them to the ManagedObjectSerializer
 */
public class ManagedObjectImpl implements ManagedObject, ManagedObjectReference, PrettyPrintable {
  private static final TCLogger            logger                   = TCLogging.getLogger(ManagedObjectImpl.class);
  private static final DNAEncodingInternal DNA_STORAGE_ENCODING     = new StorageDNAEncodingImpl();

  private final static byte                IS_NEW_OFFSET            = 1;
  private final static byte                IS_DIRTY_OFFSET          = 2;
  private final static byte                REFERENCED_OFFSET        = 4;
  private final static byte                REMOVE_ON_RELEASE_OFFSET = 8;

  private final static byte                INITIAL_FLAG_VALUE       = IS_DIRTY_OFFSET | IS_NEW_OFFSET;

  private static final long                UNINITIALIZED_VERSION    = -1;

  private final ObjectID                   id;
  private long                             version                  = UNINITIALIZED_VERSION;
  private transient ManagedObjectState     state;

  // TODO::Split this flag into two so that concurrency is maintained
  private byte                             flags                    = INITIAL_FLAG_VALUE;

  private final ManagedObjectPersistor persistor;

  public ManagedObjectImpl(final ObjectID id, ManagedObjectPersistor persistor) {
    Assert.assertNotNull(id);
    this.id = id;
    this.persistor = persistor;
  }

  /**
   * This is here for testing, not production use.
   */
  public boolean isEqual(final ManagedObject moi) {
    if (this == moi) { return true; }
    if (moi instanceof ManagedObjectImpl) {
      final ManagedObjectImpl mo = (ManagedObjectImpl) moi;
      boolean rv = true;
      rv &= this.id.equals(mo.id);
      rv &= this.version == mo.version;
      rv &= this.state.equals(mo.state);
      return rv;
    } else {
      return false;
    }
  }

  private void setBasicIsNew(final boolean b) {
    setFlag(IS_NEW_OFFSET, b);
  }

  private void setBasicIsDirty(final boolean isDirty) {
    setFlag(IS_DIRTY_OFFSET, isDirty);
  }

  private synchronized boolean compareAndSetFlag(final int offset, final boolean expected, final boolean value) {
    if (getFlag(offset) == expected) {
      this.flags = Conversion.setFlag(this.flags, offset, value);
      return true;
    }
    return false;
  }

  private synchronized void setFlag(final int offset, final boolean value) {
    this.flags = Conversion.setFlag(this.flags, offset, value);
  }

  private synchronized boolean getFlag(final int offset) {
    return (this.flags & offset) == offset;
  }

  @Override
  public boolean isNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  @Override
  public void setIsNew(final boolean isNew) {
    setBasicIsNew(isNew);
  }

  @Override
  public boolean isDirty() {
    return getFlag(IS_DIRTY_OFFSET);
  }

  @Override
  public void setIsDirty(final boolean isDirty) {
    setBasicIsDirty(isDirty);
  }

  @Override
  public ObjectID getID() {
    return this.id;
  }

  @Override
  public Set<ObjectID> getObjectReferences() {
    return (state == null ? TCCollections.EMPTY_OBJECT_ID_SET : this.state.getObjectReferences());
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    this.state.addObjectReferencesTo(traverser);
  }

  @Override
  public void apply(final DNA dna, final TransactionID txnID, final ApplyTransactionInfo applyInfo,
                    final ObjectInstanceMonitor instanceMonitor, final boolean ignoreIfOlderDNA) {
    final boolean isUninitialized = isUninitialized();

    final long dna_version = dna.getVersion();
    if (dna_version <= this.version) {
      if (ignoreIfOlderDNA) {
        if (logger.isDebugEnabled()) {
          logger.debug("Ignoring apply of an old DNA for " + getClassname() + " id = " + this.id + " current version = "
                      + this.version + " dna_version = " + dna_version);
        }
        return;
      } else {
        throw new AssertionError("Recd a DNA with version less than or equal to the current version : " + this.version
                                 + " dna_version : " + dna_version);
      }
    }
    if (dna.isDelta() && isUninitialized) {
      throw new AssertionError("Uninitalized Object is applied with a delta DNA ! ManagedObjectImpl = " + toString()
                               + " DNA = " + dna + " TransactionID = " + txnID);
    } else if (!dna.isDelta() && !isUninitialized) {
      // New DNA applied on old object - a No No for logical objects.
      throw new AssertionError("Old Object is applied with a non-delta DNA ! ManagedObjectImpl = " + toString()
                               + " DNA = " + dna + " TransactionID = " + txnID);
    }
    this.version = dna_version;
    if (isUninitialized) {
      instanceMonitor.instanceCreated(dna.getTypeName());
    }
    final DNACursor cursor = dna.getCursor();

    if (this.state == null) {
      if (!isUninitialized) {
        throw new AssertionError("Creating state on an initialized object.");
      }
      setState(getStateFactory().createState(this.id, dna.getParentObjectID(), dna.getTypeName(), cursor));
    }
    try {
      this.state.apply(this.id, cursor, applyInfo);
    } catch (final IOException e) {
      throw new DNAException(e);
    }

    // TODO: Do something about that null.
    persistor.saveObject(null, this);

    // Not unsetting isNew() flag on apply, but rather on release
    // setBasicIsNew(false);
  }

  private void setState(final ManagedObjectState newState) {
    if (this.state != null) { throw new AssertionError("Trying to set state on already initialized object : " + this
                                                       + " state : " + this.state); }
    this.state = newState;
  }

  private boolean isUninitialized() {
    return this.version == UNINITIALIZED_VERSION;
  }

  private ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

  @Override
  public ManagedObjectState getManagedObjectState() {
    return this.state;
  }

  /**
   * Writes the data in the object to the DNA strand supplied.
   */
  @Override
  public void toDNA(final TCByteBufferOutputStream out, final ObjectStringSerializer serializer, final DNAType type) {
    final DNAWriter writer = new ObjectDNAWriterImpl(out, this.id, getClassname(), serializer, DNA_STORAGE_ENCODING,
                                                     this.version, false);
    this.state.dehydrate(this.id, writer, type);
    writer.markSectionEnd();
    writer.finalizeHeader();
  }

  @Override
  public String toString() {
    final StringWriter writer = new StringWriter();
    final PrintWriter pWriter = new PrintWriter(writer);
    new PrettyPrinterImpl(pWriter).visit(this);
    return writer.getBuffer().toString();
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out = out.print("ManagedObjectImpl").duplicateAndIndent().println();
    out.indent().print("identityHashCode: " + System.identityHashCode(this)).println();
    out.indent().print("id: " + this.id).println();
    out.indent().print("className: " + getClassname()).println();
    out.indent().print("version:" + this.version).println();
    out.indent().print("state: ").visit(this.state).println();
    out.indent().print("isDirty:" + isDirty());
    out.indent().print("isNew:" + isNew());
    out.indent().print("isReferenced:" + isReferenced()).println();
    return rv;
  }

  @Override
  public ManagedObjectFacade createFacade(final int limit) {
    return this.state.createFacade(this.id, getClassname(), limit);
  }

  /**
   * This is public for testing
   */
  public String getClassname() {
    return this.state == null ? "UNKNOWN" : this.state.getClassName();
  }

  @Override
  public ManagedObjectReference getReference() {
    return this;
  }

  /*********************************************************************************************************************
   * ManagedObjectReference Interface
   */
  @Override
  public ManagedObject getObject() {
    return this;
  }

  @Override
  public boolean isRemoveOnRelease() {
    // Serialized entries are always remove on release
    return (state instanceof SerializedClusterObjectState)
            || getFlag(REMOVE_ON_RELEASE_OFFSET);
  }
  
  @Override
  public boolean markReference() {
    return compareAndSetFlag(REFERENCED_OFFSET, false, true);
  }

  @Override
  public boolean unmarkReference() {
    return compareAndSetFlag(REFERENCED_OFFSET, true, false);
  }

  @Override
  public boolean isReferenced() {
    return getFlag(REFERENCED_OFFSET);
  }

  @Override
  public void setRemoveOnRelease(final boolean removeOnRelease) {
    setFlag(REMOVE_ON_RELEASE_OFFSET, removeOnRelease);
  }

  @Override
  public ObjectID getObjectID() {
    return getID();
  }

  @Override
  public long getVersion() {
    return this.version;
  }

  void setDeserializedState(final long v, final ManagedObjectState s) {
    if (!isUninitialized() || this.state != null) { throw new AssertionError(
                                                                             "Calling setDeserializedState on initialized object : "
                                                                                 + this + " version : " + v
                                                                                 + " ManagedObjectState : " + s); }
    this.version = v;
    setState(s);
    setIsDirty(false);
    setIsNew(false);
  }

  @Override
  public void serializeTo(final ObjectOutput out, final ManagedObjectStateSerializer stateSerializer) throws IOException {
    out.writeLong(getVersion());
    out.writeLong(getObjectID().toLong());
    stateSerializer.serializeTo(getManagedObjectState(), out);
  }
}
