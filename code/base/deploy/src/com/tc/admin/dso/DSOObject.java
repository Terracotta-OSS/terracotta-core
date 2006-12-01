/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class DSOObject {
  protected ConnectionContext     m_cc;
  protected DSOObject             m_parent;
  protected PropertyChangeSupport m_changeHelper;
  protected int                   m_batchSize;
  
  public DSOObject(ConnectionContext cc) {
    m_cc           = cc;
    m_changeHelper = new PropertyChangeSupport(this);
    m_batchSize    = ConnectionContext.DSO_SMALL_BATCH_SIZE;
  }

  public DSOObject(ConnectionContext cc, DSOObject parent) {
    this(cc);
    m_parent = parent;
  }

  public abstract Object getFacade();
  public abstract void accept(DSOObjectVisitor visitor);
  
  public DSOObject getParent() {
    return m_parent;
  }

  public void setBatchSize(int batchSize) {
    m_batchSize = batchSize;
  }
  
  public abstract String getName();

  public DSORoot getRoot() {
    DSOObject obj = this;

    while(obj != null) {
      if(obj.getParent() == null) {
        return (DSORoot)obj;
      }
      obj = obj.getParent();
    }

    return null;
  }

  protected DSOObject createField(String fieldName, Object value)
    throws Exception
  {
    DSOObject result = null;
    String    type   = null;

    if(value instanceof MapEntryFacade) {
      MapEntryFacade mef = (MapEntryFacade)value;

      result = new DSOMapEntryField(m_cc, fieldName, mef, this);
    }
    else if(value instanceof ObjectID) {
      ObjectID id = (ObjectID)value;

      if(!id.isNull()) {
        value = DSOHelper.getHelper().lookupFacade(m_cc, id, m_batchSize);
        type  = ((ManagedObjectFacade)value).getClassName();
      }
      else {
        value = null;
        type  = null;
      }

      result = new DSOField(m_cc, fieldName, false, type, value, this);
    }
    else {
      type   = value.getClass().getName();
      result = new DSOField(m_cc, fieldName, true, type, value, this);
    }

    return result;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_changeHelper.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    m_changeHelper.removePropertyChangeListener(listener);
  }
}
