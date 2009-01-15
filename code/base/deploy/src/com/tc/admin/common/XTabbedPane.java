/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JTabbedPane;

public class XTabbedPane extends JTabbedPane {
  public XTabbedPane() {
    super();
  }

  public XTabbedPane(int tabPlacement) {
    super(tabPlacement);
  }
  
  public void tearDown() {
    int tabCount = getTabCount();
    for (int i = 0; i < tabCount; i++) {
      java.awt.Component comp = getComponentAt(i);
      if (comp instanceof XContainer) {
        ((XContainer) comp).tearDown();
      }
    }
    removeAll();
  }
}
