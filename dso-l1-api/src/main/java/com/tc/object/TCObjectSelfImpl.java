/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.util.ToggleableStrongReference;

import gnu.trove.TLinkable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.WeakReference;

/**
 * Classes that are merged with this class should NOT have any common names for methods, members etc with this class
 */
public class TCObjectSelfImpl implements TCObjectSelf {

  private volatile transient ObjectID oid;
  private volatile transient TCClass  tcClazz;
  private volatile transient boolean  isNew;
  private volatile transient long     version;

  // DO NOT ADD ANY CONSTRUCTORS AS THEY WILL BE SKIPPED WHILE MERGE

  public void initializeTCObject(final ObjectID id, final TCClass clazz, final boolean isNewObject) {
    oid = id;
    tcClazz = clazz;
    isNew = isNewObject;
  }

  public void serialize(ObjectOutput out) throws IOException {
    out.writeLong(version);
    out.writeLong(oid.toLong());
  }

  public void deserialize(ObjectInput in) throws IOException {
    this.version = in.readLong();
    this.oid = new ObjectID(in.readLong());
    // Assuming isNew to be false
    this.isNew = false;
  }

  public void initClazzIfRequired(TCClass tcc) {
    if (tcClazz == null) {
      tcClazz = tcc;
    }
  }

  public void dehydrate(DNAWriter writer) {
    this.tcClazz.dehydrate(this, writer, this);
  }

  public ObjectID getObjectID() {
    if (oid == null) throw new AssertionError("getObjectID() called before initialization for "
                                              + this.getClass().getName());
    return oid;
  }

  public TCClass getTCClass() {
    if (tcClazz == null) throw new AssertionError("getTCClass() called before initialization for "
                                                  + this.getClass().getName());
    return tcClazz;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long v) {
    this.version = v;
  }

  public void hydrate(DNA dna, boolean force, WeakReference peer) throws ClassNotFoundException {
    synchronized (getResolveLock()) {
      try {
        this.tcClazz.hydrate(this, dna, this, force);
      } catch (final ClassNotFoundException e) {
        throw e;
      } catch (final IOException e) {
        // TODO::Revisit
        throw new RuntimeException(e);
      }
    }
  }

  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    objectFieldChanged(classname, fieldname, Integer.valueOf(newValue), index);
  }

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    try {
      tcClazz.getObjectManager().getTransactionManager().fieldChanged(this, classname, fieldname, newValue, index);
    } catch (final Throwable t) {
      // TODO::Revisit
      throw new RuntimeException(t);
    }
  }

  public Object getPeerObject() {
    return this;
  }

  public boolean canEvict() {
    // nothing to evict as tco=self
    return false;
  }

  public Object getResolveLock() {
    return this;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNotNew() {
    isNew = false;
  }

  public void setValue(String fieldName, Object obj) {
    try {
      final TransparentAccess ta = (TransparentAccess) getPeerObject();
      if (ta == null) { throw new AssertionError(); }
      final TCField field = getTCClass().getField(fieldName);
      if (field == null) {
        // logger.warn("Data for field:" + fieldName + " was recieved but that field does not exist in class:");
        return;
      }
      if (obj instanceof ObjectID) {
        // no references should ever be cleared, as no references itself
        throw new AssertionError();
      } else {
        ta.__tc_setfield(field.getName(), obj);
      }
    } catch (final Exception e) {
      // TODO: More elegant exception handling.
      throw new com.tc.object.dna.api.DNAException(e);
    }
  }

  // ====================================================
  // TC Cache Manager methods - not needed
  // ====================================================
  public int accessCount(int arg0) {
    throw new UnsupportedOperationException();
  }

  public void clearAccessed() {
    // No-op
  }

  public boolean isCacheManaged() {
    return false;
  }

  public void markAccessed() {
    // No-op
  }

  public boolean recentlyAccessed() {
    return false;
  }

  public void clearReference(String arg0) {
    // No reference to clear
  }

  public int clearReferences(int arg0) {
    // No reference to clear
    return 0;
  }

  // ====================================================
  // Not relevant for this implementation
  // ====================================================
  public boolean autoLockingDisabled() {
    return false;
  }

  public void disableAutoLocking() {
    // No-op
  }

  public ToggleableStrongReference getOrCreateToggleRef() {
    throw new UnsupportedOperationException();
  }

  public boolean isFieldPortableByOffset(long arg0) {
    throw new UnsupportedOperationException();
  }

  public boolean isShared() {
    return true;
  }

  public void literalValueChanged(Object arg0, Object arg1) {
    // No literal value to change
    throw new UnsupportedOperationException();
  }

  public void logicalInvoke(int arg0, String arg1, Object[] arg2) {
    throw new UnsupportedOperationException();
  }

  public void booleanFieldChanged(String arg0, String arg1, boolean arg2, int arg3) {
    // No Boolean field
    throw new UnsupportedOperationException();
  }

  public void byteFieldChanged(String arg0, String arg1, byte arg2, int arg3) {
    // No byte field
    throw new UnsupportedOperationException();
  }

  public void charFieldChanged(String arg0, String arg1, char arg2, int arg3) {
    // No char field
    throw new UnsupportedOperationException();
  }

  public void longFieldChanged(String arg0, String arg1, long arg2, int arg3) {
    // No long value to change
    throw new UnsupportedOperationException();
  }

  public void objectArrayChanged(int arg0, Object[] arg1, int arg2) {
    // Not an object array
    throw new UnsupportedOperationException();
  }

  public void doubleFieldChanged(String arg0, String arg1, double arg2, int arg3) {
    // No double field
    throw new UnsupportedOperationException();
  }

  public void floatFieldChanged(String arg0, String arg1, float arg2, int arg3) {
    // No float field
    throw new UnsupportedOperationException();
  }

  public String getFieldNameByOffset(long arg0) {
    throw new UnsupportedOperationException();
  }

  public void objectFieldChangedByOffset(String arg0, long arg1, Object arg2, int arg3) {
    // Not an array
    throw new UnsupportedOperationException();

  }

  public void primitiveArrayChanged(int arg0, Object arg1, int arg2) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  public void resolveAllReferences() {
    // No-op
  }

  public void resolveArrayReference(int arg0) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  public void resolveReference(String arg0) {
    // No-op
  }

  public void setArrayReference(int arg0, ObjectID arg1) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  public void setLiteralValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  public ObjectID setReference(String arg0, ObjectID arg1) {
    throw new UnsupportedOperationException();
  }

  public void shortFieldChanged(String arg0, String arg1, short arg2, int arg3) {
    // No long value to change
    throw new UnsupportedOperationException();
  }

  public void unresolveReference(String arg0) {
    throw new UnsupportedOperationException();
  }

  // ====================================================
  // Not used anymore - needs cleanup
  // ====================================================
  public TLinkable getNext() {
    throw new UnsupportedOperationException();
  }

  public TLinkable getPrevious() {
    throw new UnsupportedOperationException();
  }

  public void setNext(TLinkable arg0) {
    throw new UnsupportedOperationException();
  }

  public void setPrevious(TLinkable arg0) {
    throw new UnsupportedOperationException();
  }

}
