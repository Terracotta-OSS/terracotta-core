/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;

import gnu.trove.TLinkable;

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

  public void setNext(TLinkable link) {
    // do nothing
  }

  public void setPrevious(TLinkable link) {
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

  public int clearReferences(int toClear) {
    return 0;
  }

  public Object getResolveLock() {
    return this;
  }

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    // do nothing
  }

  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
    // do nothing
  }

  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
    // do nothing

  }

  public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
    // do nothing
  }

  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
    // do nothing
  }

  public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
    // do nothing
  }

  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    // do nothing
  }

  public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
    // do nothing
  }

  public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
    // do nothing
  }

  public void logicalInvoke(int method, String methodName, Object[] parameters) {
    // do nothing
  }

  public void hydrate(DNA from, boolean force) throws DNAException {
    // do nothing
  }

  public void resolveReference(String fieldName) {
    // do nothing
  }

  public void resolveArrayReference(int index) {
    // do nothing
    return;
  }

  public void setReference(String fieldName, ObjectID id) {
    // do nothing
  }

  public void setValue(String fieldName, Object obj) {
    // do nothing
  }

  public long getVersion() {
    // do nothing
    return 0;
  }

  public void setVersion(long version) {
    // do nothing
  }

  public void dehydrate(DNAWriter to) throws DNAException {
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

  public void clearReference(String fieldName) {
    //
  }

  public void resolveAllReferences() {
    // throw new ImplementMe();
    // do nothing
  }

  public boolean getAndResetNew() {
    throw new AssertionError();
  }

  public void setIsNew() {
    throw new AssertionError();
  }

  public boolean isNew() {
    throw new AssertionError();
  }

  public boolean isShared() {
    return true;
  }
  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
    // do nothing
  }

  public void logicalInvoke(Object object, String methodSignature, Object[] params) {
    throw new ImplementMe();
  }

  public void disableAutoLocking() {
    throw new ImplementMe();
  }

  public boolean autoLockingDisabled() {
    return false;
  }

  public String getFieldNameByOffset(long fieldOffset) {
    throw new ImplementMe();
  }

  public boolean canEvict() {
    throw new ImplementMe();
  }

  public void objectArrayChanged(int startPos, Object[] array, int length) {
    // do nothing
  }

  public void primitiveArrayChanged(int startPos, Object array, int length) {
    // do nothing
  }

  public int accessCount(int factor) {
    throw new ImplementMe();
  }

  public void literalValueChanged(Object newValue, Object oldValue) {
    // do nothing
  }

  public void setLiteralValue(Object newValue) {
    // do nothing
  }

}
