/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
