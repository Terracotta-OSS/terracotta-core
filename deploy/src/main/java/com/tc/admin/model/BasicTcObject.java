/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.dso.DSOObjectVisitor;
import com.tc.admin.dso.RootsHelper;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.util.Arrays;

public class BasicTcObject extends AbstractTcObject implements IBasicObject {
  private Object                value;
  private boolean               isMap;
  private boolean               isList;
  private boolean               isSet;
  private boolean               isArray;
  private final String          type;
  private String[]              fieldNames;
  private IObject[]             fields;
  private String                label;
  private boolean               isCycle;
  private IObject               cycleRoot;

  private static final String[] EMPTY_FIELD_NAMES = {};

  public BasicTcObject(ManagedObjectFacadeProvider facadeProvider, String name, Object value, String type,
                       IObject parent) {
    super(facadeProvider, name, parent);
    this.value = value;
    this.type = type;
    initFields();
    updateLabel();
  }

  public BasicTcObject(BasicTcObject other) {
    super(other.facadeProvider, other.name, other.parent);
    this.value = other.value;
    this.type = other.type;
    initFields();
    updateLabel();
  }

  public IBasicObject newCopy() {
    return new BasicTcObject(this);
  }

  public Object getValue() {
    return value;
  }

  @Override
  public Object getFacade() {
    return value instanceof ManagedObjectFacade ? (ManagedObjectFacade) value : null;
  }

  @Override
  public ObjectID getObjectID() {
    if (value instanceof ManagedObjectFacade) { return ((ManagedObjectFacade) value).getObjectId(); }
    return null;
  }

  protected void updateLabel() {
    String prefix = name;

    if (type != null) {
      prefix += " (" + type + ")";
    }

    label = prefix;

    if (isCollection()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      label += " [" + mof.getFacadeSize() + "/" + mof.getTrueObjectSize() + "]";
    } else if (isArray()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      label += " [" + getFieldCount() + "/" + mof.getArrayLength() + "]";
    } else if (value instanceof EnumInstance) {
      EnumInstance enumInstance = (EnumInstance) value;
      String enumType = enumInstance.getClassInstance().getName().asString();
      label = getName() + " (" + enumType + ")" + "=" + enumInstance;
    } else if (!(value instanceof ManagedObjectFacade)) {
      label = prefix + "=" + value;
    }

    if (value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      ObjectID oid = mof.getObjectId();
      label += " [@" + oid.getMaskedObjectID() + ", gid=" + oid.getGroupID() + "]";
    }
  }

  public void initFields() {
    if (value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;

      isMap = mof.isMap();
      isList = mof.isList();
      isSet = mof.isSet();
      isArray = mof.isArray();
      isCycle = isCycle(mof);
    }

    fields = new IObject[getFieldCount()];
  }

  public int getFieldCount() {
    return isCycle ? 0 : ensureFieldNames().length;
  }

  @Override
  public String toString() {
    return label;
  }

  private String[] ensureFieldNames() {
    if (fieldNames == null) {
      if (value instanceof ManagedObjectFacade) {
        ManagedObjectFacade facade = (ManagedObjectFacade) value;
        fieldNames = RootsHelper.getHelper().trimFields(facade.getFields());
      } else {
        fieldNames = new String[] {};
      }
    }
    return fieldNames;
  }

  public String[] getFieldNames() {
    return Arrays.asList(ensureFieldNames()).toArray(EMPTY_FIELD_NAMES);
  }

  public String getFieldName(int index) {
    return ensureFieldNames()[index];
  }

  public IObject getField(int index) {
    int fieldCount = getFieldCount();
    if(index < 0 || index >= fieldCount) {
      throw new AssertionError("index not between 0 and " + fieldCount + ", value was = " + index);
    }

    if (fields == null) {
      fields = new IObject[fieldCount];
    }

    if (fields[index] == null) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      String field = getFieldName(index);

      try {
        fields[index] = newObject(field, mof.getFieldValue(field), mof.getFieldType(field));
      } catch (Throwable t) {
        /**/
      }
    }

    return fields[index];
  }

  public boolean isValid() {
    try {
      updateFacade();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  protected void updateFacade() throws Exception {
    if (value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      value = facadeProvider.lookupFacade(mof.getObjectId(), batchSize);
    }
  }

  public boolean isCycle(ManagedObjectFacade mof) {
    ObjectID oid = mof.getObjectId();
    IObject theParent = getParent();

    while (theParent != null) {
      Object theValue = theParent.getFacade();
      if (theValue instanceof ManagedObjectFacade) {
        if (oid.equals(((ManagedObjectFacade) theValue).getObjectId())) {
          cycleRoot = theParent;
          return true;
        }
      }
      theParent = theParent.getParent();
    }

    return false;
  }

  public boolean isArray() {
    return isArray;
  }

  public boolean isCollection() {
    return isMap() || isList() || isSet();
  }

  public boolean isComplete() {
    if (isCollection()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) value;
      return mof.getFacadeSize() == mof.getTrueObjectSize();
    }
    return true;
  }

  public boolean isMap() {
    return isMap;
  }

  public boolean isList() {
    return isList;
  }

  public boolean isCycle() {
    return isCycle;
  }

  public IObject getCycleRoot() {
    return cycleRoot;
  }

  public boolean isSet() {
    return isSet;
  }

  public String getType() {
    return type;
  }

  public void refresh() {
    try {
      fieldNames = null;
      fields = null;
      updateFacade();
      updateLabel();
    } catch (Exception e) {
      // JMX connection has probably been dropped
    }
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitBasicObject(this);
  }

}
