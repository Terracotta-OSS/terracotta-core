/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;

import javax.swing.JComponent;

public class ComponentNode extends XTreeNode implements IComponentProvider {
  private String    label;
  private Component component;

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
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public void setComponent(Component comp) {
    if (component instanceof XContainer) {
      ((XContainer) component).tearDown();
    }
    component = comp;
    if (comp != null) {
      if (comp instanceof JComponent) {
        ((JComponent) comp).revalidate();
      }
      comp.repaint();
    }
  }

  public Component getComponent() {
    return component;
  }

  public String toString() {
    return label != null ? label : super.toString();
  }
  
  public void tearDown() {
    super.tearDown();
    setLabel(null);
    setComponent(null);
  }
}
