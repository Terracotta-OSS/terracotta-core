/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.cache.Cacheable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

import gnu.trove.TLinkable;

public interface TCObject extends Cacheable {
  public static final Long NULL_OBJECT_ID = new Long(-1);
  public static final int  NULL_INDEX     = -1;

  public void setNext(TLinkable link);

  public void setPrevious(TLinkable link);

  public TLinkable getNext();

  public TLinkable getPrevious();

  public ObjectID getObjectID();

  public boolean isShared();

  /**
   * Returns the Object that this TCObject is wrapping. This value will be null if the peer Object is null.
   */
  public Object getPeerObject();

  /**
   * Returns the TCClass for this TCObject. The TCClass is a peer of the Class of the peer Object.
   */
  public TCClass getTCClass();

  /**
   * @param toClear - the number of references to clear atmost
   * @return - the number of references actually cleared
   */
  public int clearReferences(int toClear);

  public Object getResolveLock();

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index);

  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index);

  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index);

  public void charFieldChanged(String classname, String fieldname, char newValue, int index);

  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index);

  public void floatFieldChanged(String classname, String fieldname, float newValue, int index);

  public void intFieldChanged(String classname, String fieldname, int newValue, int index);

  public void longFieldChanged(String classname, String fieldname, long newValue, int index);

  public void shortFieldChanged(String classname, String fieldname, short newValue, int index);

  public void objectArrayChanged(int startPos, Object[] array, int length);

  public void primitiveArrayChanged(int startPos, Object array, int length);

  public void literalValueChanged(Object newValue, Object oldValue);

  public void setLiteralValue(Object newValue);

  /**
   * Takes a DNA strand and hydrates the object with it.
   *
   * @param force true if the DNA should be applied w/o any version checking
   * @throws ClassNotFoundException
   */
  public void hydrate(DNA from, boolean force) throws ClassNotFoundException;

  public ArrayIndexOutOfBoundsException checkArrayIndex(int index);

  public void resolveReference(String fieldName);

  public void resolveArrayReference(int index);

  public void resolveAllReferences();

  /**
   * @returns Old mapping if present
   */
  public ObjectID setReference(String fieldName, ObjectID id);

  public void setArrayReference(int index, ObjectID id);

  public void clearReference(String fieldName);

  public void setValue(String fieldName, Object obj);

  public long getVersion();

  public void setVersion(long version);

  public void setIsNew();

  public boolean isNew();

  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index);

  public String getFieldNameByOffset(long fieldOffset);

  public void logicalInvoke(int method, String methodSignature, Object[] params);

  public void disableAutoLocking();

  public boolean autoLockingDisabled();

  /**
   * Writers all of the object data to the given DNAWriter, iff object is new
   */

  public boolean dehydrateIfNew(DNAWriter writer);
}
