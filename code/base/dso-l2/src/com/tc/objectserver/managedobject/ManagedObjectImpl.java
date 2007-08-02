/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectDNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.managedobject.bytecode.ClassNotCompatableException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import gnu.trove.TLinkable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Set;

/**
 * Responsible for maintaining the state of a shared object. Used for broadcasting new instances of an object as well as
 * having changes applied to it and keeping track of references for garbage collection. If you add fields to this object
 * that need to be serialized make sure you add them to the ManagedObjectSerializer
 * 
 * @author steve TODO:: Remove Cacheable interface from this Object.
 */
public class ManagedObjectImpl implements ManagedObject, ManagedObjectReference, Serializable, PrettyPrintable {
  private static final TCLogger    logger                   = TCLogging.getLogger(ManagedObjectImpl.class);
  private static final DNAEncoding DNA_STORAGE_ENCODING     = new DNAEncoding(DNAEncoding.STORAGE);

  private final static byte        IS_NEW_OFFSET            = 1;
  private final static byte        IS_DIRTY_OFFSET          = 2;
  private final static byte        REFERENCED_OFFSET        = 4;
  private final static byte        REMOVE_ON_RELEASE_OFFSET = 8;
  private final static byte        PINNED_OFFSET            = 16;

  private final static byte        INITIAL_FLAG_VALUE       = IS_DIRTY_OFFSET | IS_NEW_OFFSET;

  final ObjectID                   id;

  long                             version                  = -1;
  transient ManagedObjectState     state;

  // TODO::Split this flag into two so that concurrency is maintained
  private volatile transient byte  flags                    = INITIAL_FLAG_VALUE;

  // TODO:: Remove Cacheable interface from this Object and remove these two references
  private transient TLinkable      previous;
  private transient TLinkable      next;

  private int                      accessed;

  public ManagedObjectImpl(ObjectID id) {
    Assert.assertNotNull(id);
    this.id = id;
  }

  /**
   * This is here for testing, not production use.
   */
  public boolean isEqual(ManagedObject moi) {
    if (this == moi) return true;
    if (moi instanceof ManagedObjectImpl) {
      ManagedObjectImpl mo = (ManagedObjectImpl) moi;
      boolean rv = true;
      rv &= id.equals(mo.id);
      rv &= version == mo.version;
      rv &= state.equals(mo.state);
      return rv;
    } else return false;
  }

  void setBasicIsNew(boolean b) {
    setFlag(IS_NEW_OFFSET, b);
  }

  private void setBasicIsDirty(boolean b) {
    setFlag(IS_DIRTY_OFFSET, b);
  }

  private synchronized void setFlag(int offset, boolean value) {
    flags = Conversion.setFlag(flags, offset, value);
  }

  private synchronized boolean getFlag(int offset) {
    return (flags & offset) == offset;
  }

  private boolean basicIsNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  private boolean basicIsDirty() {
    return getFlag(IS_DIRTY_OFFSET);
  }

  public boolean isNew() {
    return basicIsNew();
  }

  public boolean isDirty() {
    return basicIsDirty();
  }

  public void setIsDirty(boolean isDirty) {
    setBasicIsDirty(isDirty);
  }

  public ObjectID getID() {
    return id;
  }

  public Set getObjectReferences() {
    return state.getObjectReferences();
  }

  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    state.addObjectReferencesTo(traverser);
  }

  public void apply(DNA dna, TransactionID txnID, BackReferences includeIDs, ObjectInstanceMonitor instanceMonitor,
                    boolean ignoreIfOlderDNA) {
    boolean isNew = isNew();
    String typeName = dna.getTypeName();
    long dna_version = dna.getVersion();
    if (dna_version <= this.version) {
      if (ignoreIfOlderDNA) {
        logger.info("Ignoring apply of an old DNA for " + typeName + " id = " + id + " current version = "
                    + this.version + " dna_version = " + dna_version);
        return;
      } else {
        throw new AssertionError("Recd a DNA with version less than or equal to the current version : " + this.version
                                 + " dna_version : " + dna_version);
      }
    }
    if (dna.isDelta() && isNew) {
      throw new AssertionError("Newly created Object is applied with a delta DNA ! ManagedObjectImpl = "
                               + this.toString() + " DNA = " + dna + " TransactionID = " + txnID);
    } else if (!dna.isDelta() && !isNew) {
      // New DNA applied on old object - a No No for logical objects.
      throw new AssertionError("Old Object is applied with a non-delta DNA ! ManagedObjectImpl = " + this.toString()
                               + " DNA = " + dna + " TransactionID = " + txnID);
    }
    if (isNew) {
      instanceMonitor.instanceCreated(typeName);
    }
    this.version = dna_version;
    DNACursor cursor = dna.getCursor();

    if (state == null) {
      state = getStateFactory().createState(id, dna.getParentObjectID(), typeName, dna.getDefiningLoaderDescription(),
                                            cursor);
    }
    try {
      try {
        state.apply(id, cursor, includeIDs);
      } catch (ClassNotCompatableException cnce) {
        // reinitialize state object and try again
        reinitializeState(dna.getParentObjectID(), typeName, dna.getDefiningLoaderDescription(), cursor, state);
        state.apply(id, cursor, includeIDs);
      }
    } catch (IOException e) {
      throw new DNAException(e);
    }
    setIsDirty(true);
    setBasicIsNew(false);
  }

  private void reinitializeState(ObjectID pid, String className, String loaderDesc, DNACursor cursor,
                                 ManagedObjectState oldState) {
    state = getStateFactory().recreateState(id, pid, className, loaderDesc, cursor, oldState);
  }

  private ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (state == null) { throw new AssertionError("Null state:" + this); }
    out.defaultWriteObject();
    out.writeByte(state.getType());
    state.writeTo(out);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    byte type = in.readByte();
    this.state = getStateFactory().readManagedObjectStateFrom(in, type);
    this.setBasicIsNew(false);
    this.setBasicIsDirty(false);
  }

  public ManagedObjectState getManagedObjectState() {
    return state;
  }

  /**
   * Writes the data in the object to the DNA strand supplied.
   */
  public void toDNA(TCByteBufferOutputStream out, ObjectStringSerializer serializer) {
    DNAWriterImpl writer = new ObjectDNAWriterImpl(out, id, getClassname(), serializer, DNA_STORAGE_ENCODING,
                                                   getLoaderDescription(), version);
    state.dehydrate(id, writer);
    writer.finalizeDNA();
  }

  public String toString() {
    // XXX: Um... this is gross.
    StringWriter writer = new StringWriter();
    PrintWriter pWriter = new PrintWriter(writer);
    new PrettyPrinter(pWriter).visit(this);
    return writer.getBuffer().toString();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.print("ManagedObjectImpl").duplicateAndIndent().println();
    out.indent().print("identityHashCode: " + System.identityHashCode(this)).println();
    out.indent().print("id: " + id).println();
    out.indent().print("className: " + getClassname()).println();
    out.indent().print("version:" + version).println();
    out.indent().print("state: ").visit(state).println();
    out.indent().print("isDirty:" + this.basicIsDirty());
    out.indent().print("isNew:" + this.basicIsNew());
    out.indent().print("isReferenced:" + this.isReferenced()).println();
    out.indent().print("next: " + (this.getNext() != null) + " prev: " + (this.getPrevious() != null));
    return rv;
  }

  public ManagedObjectFacade createFacade(int limit) {
    return state.createFacade(id, getClassname(), limit);
  }

  /**
   * This is public for testing
   */
  public String getClassname() {
    return state == null ? "UNKNOWN" : state.getClassName();
  }

  public String getLoaderDescription() {
    return state == null ? "UNKNOWN" : state.getLoaderDescription();
  }

  public ManagedObjectReference getReference() {
    return this;
  }

  /*********************************************************************************************************************
   * ManagedObjectReference Interface
   */
  public ManagedObject getObject() {
    return this;
  }

  public boolean isRemoveOnRelease() {
    return getFlag(REMOVE_ON_RELEASE_OFFSET);
  }

  public void markReference() {
    setFlag(REFERENCED_OFFSET, true);
  }

  public void unmarkReference() {
    setFlag(REFERENCED_OFFSET, false);
  }

  public boolean isReferenced() {
    return getFlag(REFERENCED_OFFSET);
  }

  /**
   * Pins this reference in the cache.
   */
  public void pin() {
    setFlag(PINNED_OFFSET, true);
  }

  public void unpin() {
    setFlag(PINNED_OFFSET, false);
  }

  /**
   * Determines whether or not this reference is pinned in the ObjectManager's cache. This allows the object manager to
   * lookup multiple objects one at a time without evicting them from the cache.
   */
  public boolean isPinned() {
    return getFlag(PINNED_OFFSET);
  }

  /*********************************************************************************************************************
   * ManagedObjectReference::Cacheable interface XXX:: Most of these methods are not synchronized (except when accessing
   * the flag field which can be accessed from multiple threads) 'coz it is expected that these are called under the
   * scope of a bigger sync block (from evictionPolicy)
   */

  public void setRemoveOnRelease(boolean removeOnRelease) {
    setFlag(REMOVE_ON_RELEASE_OFFSET, removeOnRelease);
  }

  public void markAccessed() {
    accessed++;
  }

  public void clearAccessed() {
    accessed = 0;
  }

  public boolean recentlyAccessed() {
    return accessed > 0;
  }

  public int accessCount(int factor) {
    accessed = accessed / factor;
    return accessed;
  }

  public ObjectID getObjectID() {
    return getID();
  }

  public TLinkable getNext() {
    return next;
  }

  public TLinkable getPrevious() {
    return previous;
  }

  public void setNext(TLinkable next) {
    this.next = next;
  }

  public void setPrevious(TLinkable previous) {
    this.previous = previous;
  }

  public synchronized boolean canEvict() {
    return !(isPinned() || isReferenced() || isNew());
  }

  public long getVersion() {
    return version;
  }

}
