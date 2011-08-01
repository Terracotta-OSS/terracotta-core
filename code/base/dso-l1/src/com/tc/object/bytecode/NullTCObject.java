/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.util.ToggleableStrongReference;

import gnu.trove.TLinkable;

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

  public void setNext(final TLinkable link) {
    // do nothing
  }

  public void setPrevious(final TLinkable link) {
    // do nothing);

  }

  public TLinkable getNext() {
    // do nothing
    return null;
  }

  public TLinkable getPrevious() {
    // do nothing
    return null;
  }

  public ObjectID getObjectID() {
    return NULL_ID;
  }

  public Object getPeerObject() {
    // do nothing
    return null;
  }

  public TCClass getTCClass() {
    // do nothing
    return null;
  }

  public int clearReferences(final int toClear) {
    return 0;
  }

  public Object getResolveLock() {
    return this;
  }

  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    // do nothing
  }

  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    // do nothing
  }

  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    // do nothing

  }

  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    // do nothing
  }

  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    // do nothing
  }

  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    // do nothing
  }

  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    // do nothing
  }

  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    // do nothing
  }

  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    // do nothing
  }

  public void logicalInvoke(final int method, final String methodName, final Object[] parameters) {
    // do nothing
  }

  public void hydrate(final DNA from, final boolean force, WeakReference peer) throws DNAException {
    // do nothing
  }

  public void resolveReference(final String fieldName) {
    // do nothing
  }

  public void resolveArrayReference(final int index) {
    // do nothing
    return;
  }

  public ObjectID setReference(final String fieldName, final ObjectID id) {
    return null;
  }

  public void setValue(final String fieldName, final Object obj) {
    // do nothing
  }

  public long getVersion() {
    // do nothing
    return 0;
  }

  public void setVersion(final long version) {
    // do nothing
  }

  public void markAccessed() {
    //
  }

  public void clearAccessed() {
    //
  }

  public boolean recentlyAccessed() {
    return false;
  }

  public void clearReference(final String fieldName) {
    //
  }

  public void resolveAllReferences() {
    // throw new ImplementMe();
    // do nothing
  }

  public boolean isNew() {
    throw new AssertionError();
  }

  public boolean isShared() {
    return true;
  }

  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    // do nothing
  }

  public void logicalInvoke(final Object object, final String methodSignature, final Object[] params) {
    throw new ImplementMe();
  }

  public void disableAutoLocking() {
    throw new ImplementMe();
  }

  public boolean autoLockingDisabled() {
    return false;
  }

  public String getFieldNameByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  public boolean canEvict() {
    throw new ImplementMe();
  }

  public boolean isCacheManaged() {
    throw new ImplementMe();
  }

  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    // do nothing
  }

  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    // do nothing
  }

  public int accessCount(final int factor) {
    throw new ImplementMe();
  }

  public void literalValueChanged(final Object newValue, final Object oldValue) {
    // do nothing
  }

  public void setLiteralValue(final Object newValue) {
    // do nothing
  }

  public void setArrayReference(final int index, final ObjectID id) {
    //
  }

  public boolean isFieldPortableByOffset(final long fieldOffset) {
    throw new ImplementMe();
  }

  public ToggleableStrongReference getOrCreateToggleRef() {
    throw new AssertionError();
  }

  public void setNotNew() {
    throw new AssertionError();
  }

  public void dehydrate(final DNAWriter writer) {
    throw new AssertionError();
  }

  public void unresolveReference(final String fieldName) {
    //
  }
}
