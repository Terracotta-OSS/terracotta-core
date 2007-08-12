/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx.optimistic;

/**
 * In the clone objects we create for rollback we put one of these in the Managed field in order to be able to correlate
 * them with the original object they were created from.
 */

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;

import gnu.trove.TLinkable;

public class TCObjectClone implements TCObject {
  private final ObjectID                     objectID;
  private final long                         version;
  private final OptimisticTransactionManager txManager;
  private final TCClass                      tcClass;
  private final int                          arrayLength;

  public TCObjectClone(TCObject source, OptimisticTransactionManager txManager) {
    this(source, txManager, -1);
  }

  public TCObjectClone(TCObject source, OptimisticTransactionManager txManager, int arrayLength) {
    this.version = source.getVersion();
    this.objectID = source.getObjectID();
    this.txManager = txManager;
    this.tcClass = source.getTCClass();
    this.arrayLength = arrayLength;
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
    return objectID;
  }

  public Object getPeerObject() {
    throw new ImplementMe();
  }

  public TCClass getTCClass() {
    return tcClass;
  }

  public int clearReferences(int toClear) {
    throw new ImplementMe();
  }

  public Object getResolveLock() {
    return this;
  }

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    txManager.objectFieldChanged(this, classname, fieldname, newValue, index);
  }

  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Boolean(newValue), index);
  }

  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Byte(newValue), index);
  }

  public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Character(newValue), index);
  }

  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Double(newValue), index);
  }

  public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Float(newValue), index);
  }

  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Integer(newValue), index);
  }

  public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Long(newValue), index);
  }

  public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
    this.objectFieldChanged(classname, fieldname, new Short(newValue), index);
  }

  public void logicalInvoke(int method, String methodName, Object[] parameters) {
    txManager.logicalInvoke(this, method, methodName, parameters);
  }

  public void hydrate(DNA from, boolean force) throws DNAException {
    throw new ImplementMe();
  }

  public void resolveReference(String fieldName) {
    // do nothing
  }

  public void resolveArrayReference(int index) {
    // do Nothing
  }

  public boolean isShared() {
    return false;
  }

  public void resolveAllReferences() {
    // do nothing
  }

  public ObjectID setReference(String fieldName, ObjectID id) {
    throw new ImplementMe();
  }

  public void setArrayReference(int index, ObjectID id) {
    throw new ImplementMe();
  }

  public void clearReference(String fieldName) {
    throw new ImplementMe();

  }

  public void setValue(String fieldName, Object obj) {
    throw new ImplementMe();

  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    throw new ImplementMe();
  }

  public boolean dehydrateIfNew(DNAWriter writer) throws DNAException {
    throw new ImplementMe();
  }

  public void setIsNew() {
    throw new ImplementMe();
  }

  public boolean isNew() {
    return false;
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

  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
    String fieldname = tcClass.getFieldNameByOffset(fieldOffset);
    objectFieldChanged(classname, fieldname, newValue, index);
  }

  public String getFieldNameByOffset(long fieldOffset) {
    throw new ImplementMe();
  }

  public void disableAutoLocking() {
    throw new ImplementMe();
  }

  public boolean autoLockingDisabled() {
    return false;
  }

  public boolean canEvict() {
    throw new ImplementMe();
  }

  public void objectArrayChanged(int startPos, Object[] array, int length) {
    throw new ImplementMe();
  }

  public void primitiveArrayChanged(int startPos, Object arra, int lengthy) {
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
    if (arrayLength < 0) { throw new AssertionError(); }
    if (index < 0 || index >= arrayLength) { return new ArrayIndexOutOfBoundsException(index); }
    return null;
  }

}
