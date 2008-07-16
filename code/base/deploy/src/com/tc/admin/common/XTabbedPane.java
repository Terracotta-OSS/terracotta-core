/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.TabbedPane;

public class XTabbedPane extends TabbedPane {
  public XTabbedPane() {
    super();
    // setOpaque(true);
    // setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
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
