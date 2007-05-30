/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.object.LiteralValues;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.beans.PropertyChangeEvent;

import javax.management.ObjectName;

public class DSORoot extends DSOObject {
  private ObjectName                 m_bean;
  private String                     m_name;
  private ManagedObjectFacade        m_facade;
  private DSOObject[]                m_fields;
  private String                     m_label;
  private boolean                    m_isLiteral;
  
  private final static LiteralValues m_literals = new LiteralValues();

  public DSORoot(ConnectionContext cc, ObjectName bean) {
    super(cc);

    m_bean = bean;
    m_facade = safeLookupFacade();
    m_isLiteral = isLiteral();
    
    updateLabel();
  }

  public Object getFacade() {
    return m_facade;
  }
  
  protected void updateLabel() {
    m_label = getName() + " (" + getClassName() + ")";

    if(isCollection()) {
      m_label += " [" + getFacadeSize() + "/" + getTrueObjectSize() + "]";
    } else if(m_isLiteral) {
      String[] fieldNames = getFieldNames();

      if(fieldNames != null && fieldNames.length == 1) {
        Object value = getFieldValue(fieldNames[0]);

        if(value instanceof EnumInstance) {
          EnumInstance enumInstance = (EnumInstance)value;
          String enumType = enumInstance.getClassInstance().getName().asString();
          m_label = getName() + " (" + enumType + ")" + "=" + enumInstance;
        } else {
          m_label += "="+value;
        }
      }
    }
  }
  
  protected ManagedObjectFacade safeLookupFacade() {
    try {
      return lookupFacade();
    } catch(Exception e) {
      AdminClient.getContext().log(e);
    }

    return null;
  }
  
  protected ManagedObjectFacade lookupFacade()
    throws Exception
  {
    String   op    = "lookupFacade";
    String[] types = new String[]{"int"};
    Object[] args  = new Object[]{new Integer(m_batchSize)};

    return (ManagedObjectFacade)m_cc.invoke(m_bean, op, args, types);
  }

  public boolean isLiteral() {
    String className = getClassName();
    return className != null ? m_literals.isLiteral(className) || className.equals("java.util.Date") : false;
  }
  
  public String getName() {
    if(m_name == null) {
      try {
        m_name = m_cc.getStringAttribute(m_bean, "RootName");
      }
      catch(Exception e) {
        AdminClient.getContext().log(e);
      }
    }

    return m_name;
  }

  public String getClassName() {
    return m_facade != null ? m_facade.getClassName() : null;
  }

  public boolean isCollection() {
    return !m_isLiteral && (isMap() || isList() || isSet());
  }
  
  public boolean isArray() {
    return m_facade != null ? m_facade.isArray() : false;
  }

  public boolean isMap() {
    return m_facade != null ? m_facade.isMap() : false;
  }

  public boolean isList() {
    return m_facade != null ? m_facade.isList() : false;
  }

  public boolean isSet() {
    return m_facade != null ? m_facade.isSet() : false;
  }

  public int getFieldCount() {
    if(m_isLiteral) {
      return 0;
    } else {
      String[] names = getFieldNames();
      return names != null ? names.length : 0;
    }
  }

  public int getFacadeSize() {
    if(m_isLiteral) {
      return 0;
    } else {
      return m_facade != null ? m_facade.getFacadeSize() : 0;
    }
  }

  public int getTrueObjectSize() {
    if(m_isLiteral) {
      return 0;
    } else {
      return m_facade != null ? m_facade.getTrueObjectSize() : 0;
    }
  }

  public String[] getFieldNames() {
    return m_facade != null ? m_facade.getFields() : null;
  }

  public String getFieldName(int index) {
    String[] names = getFieldNames();
    return names != null ? names[index] : null;
  }

  public int getFieldIndex(String fieldName) {
    String[] names = getFieldNames();

    if(names != null) {
      for(int i = 0; i < names.length; i++) {
        if(fieldName.equals(names[i])) {
          return i;
        }
      }
    }

    return -1;
  }

  public DSOObject getField(int index) {
    if(m_fields == null) {
      m_fields = new DSOObject[getFieldCount()];
    }

    if(m_fields[index] == null) {
      m_fields[index] = newField(getFieldName(index));
    }

    return m_fields[index];
  }

  public boolean isFieldPrimitive(String fieldName) {
    return m_facade != null ? m_facade.isPrimitive(fieldName) : true;
  }

  public String getFieldType(String fieldName) {
    return m_facade != null ? m_facade.getFieldType(fieldName) : null;
  }

  public Object getFieldValue(String fieldName) {
    return m_facade != null ? m_facade.getFieldValue(fieldName) : null;
  }

  private DSOObject newField(String field) {
     try {
       return createField(field, getFieldValue(field));
     }
     catch(Exception e) {
       AdminClient.getContext().log(e);
     }

     return null;
  }

  public DSOObject getField(String fieldName) {
    return getField(getFieldIndex(fieldName));
  }

  public void refresh() {
    try {
      m_facade = lookupFacade();
      m_fields = null;
      updateLabel();

      m_changeHelper.firePropertyChange(
        new PropertyChangeEvent(this, null, null, null));
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public ObjectName getObjectName() {
    return m_bean;
  }

  public String toString() {
    return m_label;
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitDSORoot(this);
  }
}
