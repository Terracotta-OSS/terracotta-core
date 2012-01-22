/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.object.ObjectID;

import java.io.Serializable;

/**
 * Management handle to a managed object in L2 (currently used by the admin GUI)
 */
public interface ManagedObjectFacade extends Serializable {

  public String getClassName();

  public String[] getFields();

  public Object getFieldValue(String fieldName);

  public String getFieldType(String fieldName);

  public boolean isPrimitive(String fieldName);

  public ObjectID getObjectId();

  public boolean isInnerClass();

  public ObjectID getParentObjectId();

  // Methods for array type instances
  public boolean isArray();

  public int getArrayLength();

  // Methods for logically managed objects (ie. java collections like List, Map, Set, etc)
  public boolean isList();

  public boolean isSet();

  public boolean isMap();

  public int getFacadeSize(); // The current size (possibly truncated) of this collection facade

  public int getTrueObjectSize(); // The non-truncated total size of this collection

}
