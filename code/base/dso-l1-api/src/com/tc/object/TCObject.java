/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.cache.Cacheable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.util.ToggleableStrongReference;

import java.lang.ref.WeakReference;

/**
 * Terracotta class attached to each shared instance Object. The TCObject may be a simple object value or may have
 * TCFields representing internal field values.
 */
public interface TCObject extends Cacheable, TCObjectExternal {
  /** Indicates null object identifier */
  public static final Long NULL_OBJECT_ID = Long.valueOf(-1);

  /** Indicates null field index */
  public static final int  NULL_INDEX     = -1;

  /**
   * Get the object identifier
   * 
   * @return Object identifier
   */
  public ObjectID getObjectID();

  /**
   * @return True if shared (pretty much always right now)
   */
  public boolean isShared();

  /**
   * @return The Object that this TCObject is wrapping. This value will be null if the peer Object is null.
   */
  public Object getPeerObject();

  /**
   * @return The TCClass for this TCObject. The TCClass is a peer of the Class of the peer Object.
   */
  public TCClass getTCClass();

  /**
   * Clear memory references up to toClear limit
   * 
   * @param toClear - the number of references to clear atmost
   * @return - the number of references actually cleared
   */
  public int clearReferences(int toClear);

  /**
   * Get an object to lock on to modify this object.
   * 
   * @return The lock object
   */
  public Object getResolveLock();

  /**
   * Indicate that an object field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index);

  /**
   * Indicate that a boolean field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index);

  /**
   * Indicate that a byte field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index);

  /**
   * Indicate that a char field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void charFieldChanged(String classname, String fieldname, char newValue, int index);

  /**
   * Indicate that a double field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index);

  /**
   * Indicate that a float field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void floatFieldChanged(String classname, String fieldname, float newValue, int index);

  /**
   * Indicate that an int field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void intFieldChanged(String classname, String fieldname, int newValue, int index);

  /**
   * Indicate that a long field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void longFieldChanged(String classname, String fieldname, long newValue, int index);

  /**
   * Indicate that a short field has changed
   * 
   * @param classname The class name
   * @param fieldname The field name
   * @param newValue The new value
   * @param index If an array, the index into the array
   */
  public void shortFieldChanged(String classname, String fieldname, short newValue, int index);

  /**
   * Indicate that an object array changed
   * 
   * @param startPos The starting position of the change
   * @param array The changed array
   * @param length The length of the changed array
   */
  public void objectArrayChanged(int startPos, Object[] array, int length);

  /**
   * Indicate that primitive array changed
   * 
   * @param startPos The starting position of the change
   * @param array The changed array
   * @param length The length of the changed array
   */
  public void primitiveArrayChanged(int startPos, Object array, int length);

  /**
   * Indicate that a literal value changed
   * 
   * @param oldValue The old value
   * @param newValue The new value
   */
  public void literalValueChanged(Object newValue, Object oldValue);

  /**
   * Set new literal value
   * 
   * @param newValue The new value
   */
  public void setLiteralValue(Object newValue);

  /**
   * Takes a DNA strand and hydrates the object with it.
   * 
   * @param force true if the DNA should be applied w/o any version checking
   * @param weakReference
   * @throws ClassNotFoundException If class not found
   */
  public void hydrate(DNA from, boolean force, WeakReference peer) throws ClassNotFoundException;

  /**
   * Fault in field object if necessary
   * 
   * @param fieldName Fully-qualified field name
   */
  public void resolveReference(String fieldName);

  /**
   * Release this object's reference to the given field
   * 
   * @param fieldName Fully-qualified field name
   */
  public void unresolveReference(String fieldName);

  /**
   * Fault in an array reference
   * 
   * @param index Index when the peer object is an array
   */
  public void resolveArrayReference(int index);

  /**
   * Fault in all references
   */
  public void resolveAllReferences();

  /**
   * Set a reference for a field in this object
   * 
   * @param fieldName Field in this object
   * @param id New reference for this field
   * @returns Old mapping if present
   */
  public ObjectID setReference(String fieldName, ObjectID id);

  /**
   * Set an array index reference
   * 
   * @param index The index in this array
   * @param id The new reference for that index
   */
  public void setArrayReference(int index, ObjectID id);

  /**
   * Clear the reference for the given field
   * 
   * @param fieldName
   */
  public void clearReference(String fieldName);

  /**
   * Set new value for a field of this object
   * 
   * @param fieldName The field name
   * @param obj The object to set for this field
   */
  public void setValue(String fieldName, Object obj);

  /**
   * Get version of this object instance
   * 
   * @return Version
   */
  public long getVersion();

  /**
   * Set a new version for this object
   * 
   * @param version New version
   */
  public void setVersion(long version);

  /**
   * @return True if new
   */
  public boolean isNew();

  /**
   * Set a field value change by offset
   * 
   * @param classname Class name
   * @param fieldOffset The offset into this object's fields
   * @param newValue New object value
   * @param index The index if this peer object is an array
   */
  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index);

  /**
   * Get field name from field offset
   * 
   * @param fieldOffset The offset for the field
   * @return Field name
   */
  public String getFieldNameByOffset(long fieldOffset);

  /**
   * Invoke logical method
   * 
   * @param method Method indicator, as defined in {@link com.tc.object.SerializationUtil}
   * @param methodSignature The signature description
   * @param params The parameter values
   */
  public void logicalInvoke(int method, String methodSignature, Object[] params);

  /**
   * Turns off auto locking.
   */
  public void disableAutoLocking();

  /**
   * @return True if auto locking enabled.
   */
  public boolean autoLockingDisabled();

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   * 
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public boolean isFieldPortableByOffset(long fieldOffset);

  /**
   * Get or create the toggleable strong reference for this shared object. The returned object can be used to ensure the
   * peer object is strongly reachable and thus cannot be flushed by the memory manager
   */
  public ToggleableStrongReference getOrCreateToggleRef();

  /**
   * Unset the "is new" flag. This should only be done by one thread ever (namely the thread that first ever commits
   * this object)
   */
  public void setNotNew();

  /**
   * Dehydate the entire state of the peer object to the given writer
   */
  public void dehydrate(DNAWriter writer);

}
