/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.ref.WeakReference;

/**
 * Classes that are merged with this class should NOT have any common names for methods, members etc with this class
 */
public class TCObjectSelfImpl implements TCObjectSelf {

  protected volatile transient ObjectID oid;
  private volatile transient TCClass    tcClazz;
  private volatile transient boolean    isNew;
  private volatile transient long       version;

  // DO NOT ADD ANY CONSTRUCTORS AS THEY WILL BE SKIPPED WHILE MERGE

  @Override
  public void initializeTCObject(final ObjectID id, final TCClass clazz, final boolean isNewObject) {
    if (oid != null) { throw new AssertionError("Old oid=" + oid + " id=" + id); }
    oid = id;
    tcClazz = clazz;
    isNew = isNewObject;
  }

  @Override
  public void serialize(ObjectOutput out) throws IOException {
    out.writeLong(version);
    out.writeLong(oid.toLong());
  }

  @Override
  public void deserialize(ObjectInput in) throws IOException {
    this.version = in.readLong();
    ObjectID newId = new ObjectID(in.readLong());
    if (oid != null) { throw new AssertionError("Old oid=" + oid + " id=" + newId); }
    this.oid = newId;
    // Assuming isNew to be false
    this.isNew = false;
    if (this instanceof Manageable) {
      ((Manageable) this).__tc_managed(this);
    }
  }

  @Override
  public void initClazzIfRequired(TCClass tcc) {
    if (tcClazz == null) {
      tcClazz = tcc;
    }
  }

  @Override
  public void dehydrate(DNAWriter writer) {
    this.tcClazz.dehydrate(this, writer, this);
  }

  @Override
  public ObjectID getObjectID() {
    if (oid == null) throw new AssertionError("getObjectID() called before initialization for "
                                              + this.getClass().getName());
    return oid;
  }

  protected TCClass getTCClass() {
    if (tcClazz == null) throw new AssertionError("getTCClass() called before initialization for "
                                                  + this.getClass().getName());
    return tcClazz;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public void setVersion(long v) {
    this.version = v;
  }

  @Override
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

  @Override
  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    throw new UnsupportedOperationException();
    // objectFieldChanged(classname, fieldname, Integer.valueOf(newValue), index);
  }

  @Override
  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    // try {
    // tcClazz.getObjectManager().getTransactionManager().fieldChanged(this, classname, fieldname, newValue, index);
    // } catch (final Throwable t) {
    // // TODO::Revisit
    // throw new RuntimeException(t);
    // }
    throw new UnsupportedOperationException();
  }

  @Override
  public void logicalInvoke(LogicalOperation method, Object[] parameters) {
    tcClazz.getObjectManager().getTransactionManager().logicalInvoke(this, method, parameters);
  }

  @Override
  public Object getPeerObject() {
    return this;
  }

  @Override
  public Object getResolveLock() {
    return this;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @Override
  public void setNotNew() {
    isNew = false;
  }

  @Override
  public void setValue(String fieldName, Object obj) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  // ====================================================
  // TC Cache Manager methods - not needed
  // ====================================================
  @Override
  public int accessCount(int arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearAccessed() {
    // No-op
  }

  @Override
  public void markAccessed() {
    // No-op
  }

  @Override
  public boolean recentlyAccessed() {
    return false;
  }

  @Override
  public void clearReference(String arg0) {
    // No reference to clear
  }

  // ====================================================
  // Not relevant for this implementation
  // ====================================================
  @Override
  public boolean autoLockingDisabled() {
    return false;
  }

  @Override
  public void disableAutoLocking() {
    // No-op
  }

  public boolean isFieldPortableByOffset(long arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isShared() {
    return true;
  }

  @Override
  public void literalValueChanged(Object arg0, Object arg1) {
    // No literal value to change
    throw new UnsupportedOperationException();
  }

  @Override
  public void booleanFieldChanged(String arg0, String arg1, boolean arg2, int arg3) {
    // No Boolean field
    throw new UnsupportedOperationException();
  }

  @Override
  public void byteFieldChanged(String arg0, String arg1, byte arg2, int arg3) {
    // No byte field
    throw new UnsupportedOperationException();
  }

  @Override
  public void charFieldChanged(String arg0, String arg1, char arg2, int arg3) {
    // No char field
    throw new UnsupportedOperationException();
  }

  @Override
  public void longFieldChanged(String arg0, String arg1, long arg2, int arg3) {
    // No long value to change
    throw new UnsupportedOperationException();
  }

  @Override
  public void objectArrayChanged(int arg0, Object[] arg1, int arg2) {
    // Not an object array
    throw new UnsupportedOperationException();
  }

  @Override
  public void doubleFieldChanged(String arg0, String arg1, double arg2, int arg3) {
    // No double field
    throw new UnsupportedOperationException();
  }

  @Override
  public void floatFieldChanged(String arg0, String arg1, float arg2, int arg3) {
    // No float field
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFieldNameByOffset(long arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void objectFieldChangedByOffset(String arg0, long arg1, Object arg2, int arg3) {
    // Not an array
    throw new UnsupportedOperationException();

  }

  @Override
  public void primitiveArrayChanged(int arg0, Object arg1, int arg2) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  @Override
  public void resolveAllReferences() {
    // No-op
  }

  @Override
  public void resolveArrayReference(int arg0) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  @Override
  public void resolveReference(String arg0) {
    // No-op
  }

  @Override
  public void setArrayReference(int arg0, ObjectID arg1) {
    // Not an array
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLiteralValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ObjectID setReference(String arg0, ObjectID arg1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shortFieldChanged(String arg0, String arg1, short arg2, int arg3) {
    // No long value to change
    throw new UnsupportedOperationException();
  }

  @Override
  public void unresolveReference(String arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInitialized() {
    if (oid == null) { return false; }
    return true;
  }

  @Override
  public String getExtendingClassName() {
    return getClassName();
  }

  @Override
  public String getClassName() {
    return getTCClass().getName();
  }

  @Override
  public Class<?> getPeerClass() {
    return getTCClass().getPeerClass();
  }

  @Override
  public boolean isIndexed() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean isLogical() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean isEnum() {
    throw new AssertionError(); // XXX: remove method when possible
  }
}
