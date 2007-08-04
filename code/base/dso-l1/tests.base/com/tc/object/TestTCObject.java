/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;

import gnu.trove.TLinkable;

/**
 * This is a plain vanilla mock object with no internal behavior, unlinke MockTCObject
 */
public class TestTCObject implements TCObject {

  private final Object peerObject;
  private boolean      isNew = false;
  private TCClass      tcClass;

  public TestTCObject(Object myObject) {
    this.peerObject = myObject;
  }

  public void setNext(TLinkable link) {
    throw new ImplementMe();

  }

  public void setPrevious(TLinkable link) {
    throw new ImplementMe();

  }

  public TLinkable getNext() {
    throw new ImplementMe();
  }

  public TLinkable getPrevious() {
    throw new ImplementMe();
  }

  public ObjectID getObjectID() {
    return null;
  }

  public Object getPeerObject() {
    return peerObject;
  }

  public void setTCClass(TCClass clazz) {
    this.tcClass = clazz;
  }

  public TCClass getTCClass() {
    return this.tcClass;
  }

  public int clearReferences(int toClear) {
    throw new ImplementMe();
  }

  public Object getResolveLock() {
    throw new ImplementMe();
  }

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();

  }

  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
    throw new ImplementMe();

  }

  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
    throw new ImplementMe();

  }

  public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
    throw new ImplementMe();

  }

  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
    throw new ImplementMe();

  }

  public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
    throw new ImplementMe();

  }

  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    throw new ImplementMe();

  }

  public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
    throw new ImplementMe();

  }

  public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
    throw new ImplementMe();

  }

  public void logicalInvoke(int method, String methodName, Object[] parameters) {
    throw new ImplementMe();

  }

  public void hydrate(DNA from, boolean force) throws DNAException {
    throw new ImplementMe();

  }

  public void resolveReference(String fieldName) {
    throw new ImplementMe();

  }

  public void resolveArrayReference(int index) {
    throw new ImplementMe();

  }

  public void setReference(String fieldName, ObjectID id) {
    throw new ImplementMe();

  }

  public void clearReference(String fieldName) {
    throw new ImplementMe();

  }

  public void setValue(String fieldName, Object obj) {
    throw new ImplementMe();

  }

  public long getVersion() {
    throw new ImplementMe();
  }

  public void setVersion(long version) {
    throw new ImplementMe();

  }

  public void dehydrate(DNAWriter writer) throws DNAException {
    throw new ImplementMe();

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

  public void resolveAllReferences() {
    throw new ImplementMe();
  }

  public boolean getAndResetNew() {
    boolean rv = isNew;
    this.isNew = false;
    return rv;
  }

  public void setIsNew() {
    this.isNew = true;
  }

  public boolean isNew() {
    return this.isNew;
  }

  public boolean isShared() {
    return true;
  }

  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
    throw new ImplementMe();
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
    throw new ImplementMe();
  }

  public void primitiveArrayChanged(int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public int accessCount(int factor) {
    throw new ImplementMe();
  }

  public void literalValueChanged(Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  public void setLiteralValue(Object newValue) {
    throw new ImplementMe();
  }

  public ArrayIndexOutOfBoundsException checkArrayIndex(int index) {
    throw new ImplementMe();
  }

  public void setArrayReference(int index, ObjectID id) {
    throw new ImplementMe();
  }
}
