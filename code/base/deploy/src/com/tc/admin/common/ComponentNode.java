/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Component;

public class ComponentNode extends XTreeNode {
  private String    m_label;
  private Component m_component;

  public ComponentNode() {
    super();
  }    

  public ComponentNode(String label) {
    this(label, null);
  }

  public ComponentNode(String label, Component component) {
    this();
      
    setLabel(label);
    setComponent(component);
  }

  public void setLabel(String label) {
    setUserObject(m_label = label);
  }

  public String getLabel() {
    return m_label;
  }

  public void setComponent(Component comp) {
    m_component = comp;
  }
    
  public Component getComponent() {
    return m_component;
  }

  public void tearDown() {
    super.tearDown();

    setLabel(null);
    
    if(m_component instanceof XContainer) {
      ((XContainer)m_component).tearDown();
    }
    setComponent(null);
  }
}
  
