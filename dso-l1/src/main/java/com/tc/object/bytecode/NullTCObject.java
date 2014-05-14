/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;

import java.lang.ref.WeakReference;

/**
 * Null impl of TCObject
 */
public class NullTCObject implements TCObject {

  public final static TCObject  INSTANCE = new NullTCObject();

  private final static ObjectID NULL_ID  = ObjectID.NULL_ID;

  // Used by bytecode stuff so won't show up in compiler
  public static TCObject getNullTCObject() {
    return INSTANCE;
  }

  @Override
  public ObjectID getObjectID() {
    return NULL_ID;
  }

  @Override
  public Object getPeerObject() {
    // do nothing
    return null;
  }

  @Override
  public Object getResolveLock() {
    return this;
  }

  @Override
  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    // do nothing
  }

  @Override
  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    // do nothing
  }

  @Override
  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    // do nothing

  }

  @Override
  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    // do nothing
  }

  @Override
  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    // do nothing
  }

  @Override
  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    // do nothing
  }

  @Override
  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    // do nothing
  }

  @Override
  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    // do nothing
  }

  @Override
  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    // do nothing
  }

  @Override
  public void logicalInvoke(final LogicalOperation method, final Object[] parameters) {
    // do nothing
  }

  @Override
  public void hydrate(final DNA from, final boolean force, WeakReference peer) throws DNAException {
    // do nothing
  }

  @Override
  public void resolveReference(final String fieldName) {
    // do nothing
  }

  @Override
  public void resolveArrayReference(final int index) {
    // do nothing
    return;
  }

  @Override
  public ObjectID setReference(final String fieldName, final ObjectID id) {
    return null;
  }

  @Override
  public void setValue(final String fieldName, final Object obj) {
    // do nothing
  }

  @Override
  public long getVersion() {
    // do nothing
    return 0;
  }

  @Override
  public void setVersion(final long version) {
    // do nothing
  }

  @Override
  public void markAccessed() {
    //
  }

  @Override
  public void clearAccessed() {
    //
  }

  @Override
  public boolean recentlyAccessed() {
    return false;
  }

  @Override
  public void clearReference(final String fieldName) {
    //
  }

  @Override
  public void resolveAllReferences() {
    // throw new ImplementMe();
    // do nothing
  }

  @Override
  public boolean isNew() {
    throw new AssertionError();
  }

  @Override
  public boolean isShared() {
    return true;
  }

  @Override
  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    // do nothing
  }

  public void logicalInvoke(final Object object, final String methodSignature, final Object[] params) {
    throw new ImplementMe();
  }

  @Override
  public void disableAutoLocking() {
    throw new ImplementMe();
  }

  @Override
  public boolean autoLockingDisabled() {
    return false;
  }

  @Override
  public String getFieldNameByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  @Override
  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    // do nothing
  }

  @Override
  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    // do nothing
  }

  @Override
  public int accessCount(final int factor) {
    throw new ImplementMe();
  }

  @Override
  public void literalValueChanged(final Object newValue, final Object oldValue) {
    // do nothing
  }

  @Override
  public void setLiteralValue(final Object newValue) {
    // do nothing
  }

  @Override
  public void setArrayReference(final int index, final ObjectID id) {
    //
  }

  public boolean isFieldPortableByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  @Override
  public void setNotNew() {
    throw new AssertionError();
  }

  @Override
  public void dehydrate(final DNAWriter writer) {
    throw new AssertionError();
  }

  @Override
  public void unresolveReference(final String fieldName) {
    //
  }

  @Override
  public String getExtendingClassName() {
    throw new AssertionError();
  }

  @Override
  public String getClassName() {
    throw new AssertionError();
  }

  @Override
  public Class<?> getPeerClass() {
    throw new AssertionError();
  }

  @Override
  public boolean isIndexed() {
    throw new AssertionError();
  }

  @Override
  public boolean isLogical() {
    throw new AssertionError();
  }

  @Override
  public boolean isEnum() {
    throw new AssertionError();
  }
}
