/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;
import java.awt.Cursor;

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
    nodeChanged();
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

  @Override
  public String toString() {
    return label != null ? label : super.toString();
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (component != null) {
      component.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.DEFAULT_CURSOR : Cursor.WAIT_CURSOR));
    }
  }

  @Override
  public void tearDown() {
    super.tearDown();
    setLabel(null);
    setComponent(null);
  }
}
