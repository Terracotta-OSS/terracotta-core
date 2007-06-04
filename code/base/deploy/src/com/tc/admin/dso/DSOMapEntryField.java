/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.objectserver.mgmt.MapEntryFacade;

public class DSOMapEntryField extends DSOObject {
  private String         m_name;
  private MapEntryFacade m_facade;
  private DSOObject      m_key;
  private DSOObject      m_value;
  private String         m_label;

  private static final String TYPE =
    AdminClient.getContext().getMessage("map.entry");

  public DSOMapEntryField(
    ConnectionContext cc,
    String            name,
    MapEntryFacade    facade,
    DSOObject         parent)
  {
    super(cc, parent);

    m_name   = name;
    m_facade = facade;
    m_label  = m_name + " (" + TYPE + ")";
  }

  public Object getFacade() {
    return m_facade;
  }
  
  public String getName() {
    return m_name;
  }

  public DSOObject getKey() {
    if(m_key == null) {
      m_key = getElement("key", m_facade.getKey());
    }

    return m_key;
  }

  public DSOObject getValue() {
    if(m_value == null) {
      m_value = getElement("value", m_facade.getValue());
    }

    return m_value;
  }

  private DSOObject getElement(String field, Object value) {
    try {
      return createField(field, value, null);
    }
    catch(Throwable t) {
      t.printStackTrace();
    }

    return null;
  }

  public String toString() {
    return m_label;
  }

  public void accept(DSOObjectVisitor visitor) {
    visitor.visitDSOMapEntryField(this);
  }
}
