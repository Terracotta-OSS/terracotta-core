/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.beans.PropertyChangeEvent;

public class DSOField extends DSOObject {
  private String      m_name;
  private boolean     m_isPrimitive;
  private boolean     m_isMap;
  private boolean     m_isList;
  private boolean     m_isSet;
  private boolean     m_isArray;
  private String      m_type;
  private Object      m_value;
  private String[]    m_fieldNames;
  private DSOObject[] m_fields;
  private String      m_label;
  private boolean     m_isCycle;
  private DSOObject   m_cycleRoot;

  public DSOField(
    ConnectionContext cc,
    String            name,
    boolean           isPrimitive,
    String            type,
    Object            value,
    DSOObject         parent)
  {
    super(cc, parent);

    m_name        = name;
    m_isPrimitive = isPrimitive;
    m_type        = type;
    m_value       = value;

    initFields();
    updateLabel();
  }

  public Object getFacade() {
    return m_value instanceof ManagedObjectFacade ? (ManagedObjectFacade)m_value : null;
  }
  
  protected void updateLabel() {
    String prefix = m_name;

    if(m_type != null) {
      prefix += " (" + m_type + ")";
    }
    
    m_label = prefix;

    if(isPrimitive() || m_value == null || m_value instanceof String) {
      m_label = prefix + "=" + m_value;
    }
    else if(isCollection()) {
      ManagedObjectFacade mof = (ManagedObjectFacade)m_value;
      m_label += " [" + mof.getFacadeSize() + "/" + mof.getTrueObjectSize() + "]";
    }
  }
  
  public void initFields() {
    if(!m_isPrimitive) {
      if(m_value != null && m_value instanceof ManagedObjectFacade) {
        ManagedObjectFacade mof = (ManagedObjectFacade)m_value;

        m_isMap   = mof.isMap();
        m_isList  = mof.isList();
        m_isSet   = mof.isSet();
        m_isArray = mof.isArray();
        m_isCycle = isCycle(mof);
      }

      m_fields = new DSOObject[getFieldCount()];
    }
  }

  public String getName() {
    return m_name;
  }

  public boolean isPrimitive() {
    return m_isPrimitive;
  }

  public boolean isArray() {
    return m_isArray;
  }

  public boolean isCollection() {
    return isMap() || isList() || isSet();
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
  
  public DSOObject getCycleRoot() {
    return m_cycleRoot;
  }
  
  public boolean isSet() {
    return m_isSet;
  }

  public String getType() {
    return m_type;
  }

  public Object getValue() {
    return m_value;
  }

  public String[] getFieldNames() {
    if(m_fieldNames == null) {
      if(m_value != null && m_value instanceof ManagedObjectFacade) {
        ManagedObjectFacade facade = (ManagedObjectFacade)m_value;
        m_fieldNames = RootsHelper.getHelper().trimFields(facade.getFields());
      }
      else {
        m_fieldNames = new String[]{};
      }
    }
    
    return m_fieldNames;
  }

  public String getFieldName(int index) {
    String[] names = getFieldNames();
    return names != null ? names[index] : null;
  }

  public DSOObject getField(int index) {
    DSOObject result = null;

    if(m_fields == null) {
      m_fields = new DSOObject[getFieldCount()];
    }
    
    if(m_fields[index] == null) {
      ManagedObjectFacade mof   = (ManagedObjectFacade)getValue();
      String              field = getFieldName(index);

      try {
        result = createField(field, mof.getFieldValue(field));
      } catch(Throwable t) {
        //t.printStackTrace();
      }
    }

    m_fields[index] = result;

    return result;
  }

  public int getFieldCount() {
    return m_isCycle ? 0 : getFieldNames().length;
  }

  public String toString() {
    return m_label;
  }

  public boolean isValid() {
    boolean result = true;

    if(m_value != null && m_value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade)m_value;
      ObjectID            id  = mof.getObjectId();

      try {
        m_value = DSOHelper.getHelper().lookupFacade(m_cc, id, m_batchSize);
        result  = (m_value != null);
      } catch(Exception e) {
        result = false;
      }
    }

    return result;
  }
  
  protected void updateFacade() throws Exception {
    if(m_value != null && m_value instanceof ManagedObjectFacade) {
      ManagedObjectFacade mof = (ManagedObjectFacade)m_value;
      ObjectID            id  = mof.getObjectId();

      m_value = DSOHelper.getHelper().lookupFacade(m_cc, id, m_batchSize);
    }
  }
  
  public boolean isCycle(ManagedObjectFacade mof) {
    ObjectID  oid    = mof.getObjectId();
    DSOObject parent = getParent();
    
    while(parent != null) {
      Object facade = parent.getFacade();
        
      if(facade instanceof ManagedObjectFacade) {
        ObjectID parentOID = ((ManagedObjectFacade)facade).getObjectId();
        
        if(oid.equals(parentOID)) {
          m_cycleRoot = parent;
          return true;
        }
      }
      parent = parent.getParent();
    }
    
    return false;
  }
  
  public void refresh() {
    try {
      updateFacade();
      updateLabel();
      m_fieldNames = null;
      m_fields = null;
      
      m_changeHelper.firePropertyChange(
        new PropertyChangeEvent(this, null, null, null));
    } catch(Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitDSOField(this);
  }
}
