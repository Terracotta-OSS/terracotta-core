/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Component;

import javax.swing.JTabbedPane;

public class XTabbedPane extends JTabbedPane {
  public XTabbedPane() {
    super();
  }

  public XTabbedPane(int tabPlacement) {
    super(tabPlacement);
  }

  private void tearDown(Component c) {
    if (c instanceof XContainer) {
      ((XContainer) c).tearDown();
    } else if (c instanceof XSplitPane) {
      ((XSplitPane) c).tearDown();
    }
  }

  public void tearDown() {
    int tabCount = getTabCount();
    for (int i = 0; i < tabCount; i++) {
      java.awt.Component comp = getComponentAt(i);
      if (comp != null) {
        tearDown(comp);
      }
    }
    removeAll();
  }
}
