/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.object.ObjectID;

public class LogicalManagedObjectFacade extends AbstractObjectFacade {
  private static final int MAP  = 1;
  private static final int LIST = 2;
  private static final int SET  = 3;

  private final boolean    isMap;
  private final boolean    isSet;
  private final boolean    isList;
  private final int        trueSize;
  private final ObjectID   objectID;
  private final String     className;
  private final int        facadeSize;
  private final Object[]   data;

  public static LogicalManagedObjectFacade createSetInstance(ObjectID id, String className, Object[] data, int trueSize) {
    return new LogicalManagedObjectFacade(id, className, SET, data, trueSize);
  }

  public static LogicalManagedObjectFacade createListInstance(ObjectID id, String className, Object[] data, int trueSize) {
    return new LogicalManagedObjectFacade(id, className, LIST, data, trueSize);
  }

  public static LogicalManagedObjectFacade createMapInstance(ObjectID id, String className, MapEntryFacade[] data,
                                                             int trueSize) {
    return new LogicalManagedObjectFacade(id, className, MAP, data, trueSize);
  }

  private LogicalManagedObjectFacade(ObjectID objectID, String className, int type, Object[] data, int trueObjectSize) {
    if (type != MAP && type != SET && type != LIST) { throw new IllegalArgumentException("Bad type: " + type); }
    this.isMap = type == MAP;
    this.isSet = type == SET;
    this.isList = type == LIST;
    this.objectID = objectID;
    this.className = className;
    this.data = data;
    this.facadeSize = data.length;
    this.trueSize = trueObjectSize;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public String[] getFields() {
    String[] names = new String[this.facadeSize];
    for (int i = 0; i < facadeSize; i++) {
      names[i] = String.valueOf(i);
    }
    return names;
  }

  @Override
  protected Object basicGetFieldValue(String fieldName) {
    int index = Integer.valueOf(fieldName).intValue();
    return this.data[index];
  }

  @Override
  public boolean isPrimitive(String fieldName) {
    // Logical classes cannot have "primitive" fields
    return false;
  }

  @Override
  public ObjectID getObjectId() {
    return this.objectID;
  }

  @Override
  public boolean isInnerClass() {
    return false;
  }

  @Override
  public ObjectID getParentObjectId() {
    throw new IllegalStateException("Not an inner class");
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public int getArrayLength() {
    throw new IllegalStateException("Not an array");
  }

  @Override
  public boolean isList() {
    return this.isList;
  }

  @Override
  public boolean isSet() {
    return this.isSet;
  }

  @Override
  public boolean isMap() {
    return this.isMap;
  }

  @Override
  public int getFacadeSize() {
    return this.facadeSize;
  }

  @Override
  public int getTrueObjectSize() {
    return this.trueSize;
  }

}
