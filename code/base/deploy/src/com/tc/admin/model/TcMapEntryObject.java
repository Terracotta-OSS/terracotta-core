/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.AdminClient;
import com.tc.admin.dso.DSOObjectVisitor;
import com.tc.objectserver.mgmt.MapEntryFacade;

public class TcMapEntryObject extends AbstractTcObject implements IMapEntry {
  private MapEntryFacade      m_facade;
  private IObject             m_key;
  private IObject             m_value;
  private String              m_label;

  private static final String TYPE = AdminClient.getContext().getMessage("map.entry");

  public TcMapEntryObject(ManagedObjectFacadeProvider facadeProvider, String name, MapEntryFacade facade, IObject parent) {
    super(facadeProvider, name, parent);
    m_name = name;
    m_facade = facade;
    m_label = m_name + " (" + TYPE + ")";
  }

  public Object getFacade() {
    return m_facade;
  }

  public IObject getKey() {
    if (m_key == null) {
      m_key = getElement("key", m_facade.getKey());
    }

    return m_key;
  }

  public IObject getValue() {
    if (m_value == null) {
      m_value = getElement("value", m_facade.getValue());
    }

    return m_value;
  }

  private IObject getElement(String field, Object value) {
    try {
      return newObject(field, value, null);
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return null;
  }

  public String toString() {
    return m_label;
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitMapEntry(this);
  }

}
