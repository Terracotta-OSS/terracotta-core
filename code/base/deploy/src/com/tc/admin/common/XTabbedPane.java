/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JTabbedPane;
import org.dijon.TabbedPane;

public class XTabbedPane extends TabbedPane {
  public XTabbedPane() {
    super();
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
  }
}
