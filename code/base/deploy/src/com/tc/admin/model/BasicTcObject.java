/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.AdminClient;
import com.tc.admin.dso.DSOObjectVisitor;
import com.tc.admin.dso.RootsHelper;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.util.Arrays;

public class BasicTcObject extends AbstractTcObject implements IBasicObject {
  private Object                m_value;
  private boolean               m_isMap;
  private boolean               m_isList;
  private boolean               m_isSet;
  private boolean               m_isArray;
  private String                m_type;
  private String[]              m_fieldNames;
  private IObject[]             m_fields;
  private String                m_label;
  private boolean               m_isCycle;
  private IObject               m_cycleRoot;

  private static final String[] EMPTY_FIELD_NAMES = {};

  public BasicTcObject(ManagedObjectFacadeProvider facadeProvider, String name, Object value, String type,
                       IObject parent) {
    super(facadeProvider, name, parent);
    m_value = value;
    m_name = name;
    m_type = type;
    initFields();
    updateLabel();
  }

  public Object getValue() {
    return m_value;
  }

  public Object getFacade() {
    return m_value instanceof ManagedObjectFacade ? (ManagedObjectFacade) m_value : null;
  }

  public ObjectID getObjectID() {
    if (m_value instanceof ManagedObjectFacade) { return ((ManagedObjectFacade) m_value).getObjectId(); }
    return null;
  }

  protected void updateLabel() {
    String prefix = m_name;

    if (m_type != null) {
      prefix += " (" + m_type + ")";
    }

    m_label = prefix;

    if (isCollection()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      m_label += " [" + mof.getFacadeSize() + "/" + mof.getTrueObjectSize() + "]";
    } else if (isArray()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      m_label += " [" + getFieldCount() + "/" + mof.getArrayLength() + "]";
    } else if (m_value instanceof EnumInstance) {
      EnumInstance enumInstance = (EnumInstance) m_value;
      String enumType = enumInstance.getClassInstance().getName().asString();
      m_label = getName() + " (" + enumType + ")" + "=" + enumInstance;
    } else if (!(m_value instanceof ManagedObjectFacade)) {
      m_label = prefix + "=" + m_value;
    }

    if (m_value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      m_label += " [@" + mof.getObjectId().toLong() + "]";
    }
  }

  public void initFields() {
    if (m_value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;

      m_isMap = mof.isMap();
      m_isList = mof.isList();
      m_isSet = mof.isSet();
      m_isArray = mof.isArray();
      m_isCycle = isCycle(mof);
    }

    m_fields = new IObject[getFieldCount()];
  }

  public String getName() {
    return m_name;
  }

  public int getFieldCount() {
    return m_isCycle ? 0 : ensureFieldNames().length;
  }

  public String toString() {
    return m_label;
  }

  private String[] ensureFieldNames() {
    if (m_fieldNames == null) {
      if (m_value instanceof ManagedObjectFacade) {
        ManagedObjectFacade facade = (ManagedObjectFacade) m_value;
        m_fieldNames = RootsHelper.getHelper().trimFields(facade.getFields());
      } else {
        m_fieldNames = new String[] {};
      }
    }
    return m_fieldNames;
  }
  
  public String[] getFieldNames() {
    return Arrays.asList(ensureFieldNames()).toArray(EMPTY_FIELD_NAMES);
  }

  public String getFieldName(int index) {
    return ensureFieldNames()[index];
  }

  public IObject getField(int index) {
    IObject result = null;

    if (m_fields == null) {
      m_fields = new IObject[getFieldCount()];
    }

    if (m_fields[index] == null) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      String field = getFieldName(index);

      try {
        result = newObject(field, mof.getFieldValue(field), mof.getFieldType(field));
      } catch (Throwable t) {
        /**/
      }
    }

    m_fields[index] = result;

    return result;
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
    if (m_value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      m_value = m_facadeProvider.lookupFacade(mof.getObjectId(), m_batchSize);
    }
  }

  public boolean isCycle(ManagedObjectFacade mof) {
    ObjectID oid = mof.getObjectId();
    IObject parent = getParent();

    while (parent != null) {
      Object value = parent.getFacade();
      if (value instanceof ManagedObjectFacade) {
        if (oid.equals(((ManagedObjectFacade) value).getObjectId())) {
          m_cycleRoot = parent;
          return true;
        }
      }
      parent = parent.getParent();
    }

    return false;
  }

  public boolean isArray() {
    return m_isArray;
  }

  public boolean isCollection() {
    return isMap() || isList() || isSet();
  }

  public boolean isComplete() {
    if (isCollection()) {
      ManagedObjectFacade mof = (ManagedObjectFacade) m_value;
      return mof.getFacadeSize() == mof.getTrueObjectSize();
    }
    return true;
  }

  public boolean isMap() {
    return m_isMap;
  }

  public boolean isList() {
    return m_isList;
  }

  public boolean isCycle() {
    return m_isCycle;
  }

  public IObject getCycleRoot() {
    return m_cycleRoot;
  }

  public boolean isSet() {
    return m_isSet;
  }

  public String getType() {
    return m_type;
  }

  public void refresh() {
    try {
      m_fieldNames = null;
      m_fields = null;
      updateFacade();
      updateLabel();
    } catch (Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitBasicObject(this);
  }

}
