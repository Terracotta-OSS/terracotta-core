package com.tc.object;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public interface TCObjectExternal {

  Object getResolveLock();

  void resolveArrayReference(int index);

  void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index);

  void primitiveArrayChanged(int startPos, Object array, int length);

  void objectArrayChanged(int startPos, Object[] array, int length);

  void byteFieldChanged(String classname, String fieldname, byte newValue, int index);

  void charFieldChanged(String classname, String fieldname, char newValue, int index);

  void doubleFieldChanged(String classname, String fieldname, double newValue, int index);

  void floatFieldChanged(String classname, String fieldname, float newValue, int index);

  void intFieldChanged(String classname, String fieldname, int newValue, int index);

  void longFieldChanged(String classname, String fieldname, long newValue, int index);

  void shortFieldChanged(String classname, String fieldname, short newValue, int index);

  void objectFieldChanged(String classname, String fieldname, Object newValue, int index);

  String getFieldNameByOffset(long fieldOffset);

  void clearAccessed();

  ObjectID getObjectID();

  boolean autoLockingDisabled();

  void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index);

  boolean recentlyAccessed();

  void resolveAllReferences();

  int clearReferences(int size);

  void disableAutoLocking();

  void setArrayReference(int index, ObjectID value);

  void clearReference(String fieldName);

  void setValue(String fieldName, Object fieldValue);

  void setLiteralValue(Object value);

}
