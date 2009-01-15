/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;


import java.awt.Component;

import javax.swing.JScrollPane;

public class XScrollPane extends JScrollPane {
  public XScrollPane() {
    super();
  }

  public XScrollPane(Component item) {
    super(item);
  }

  public XScrollPane(Component item, int verticalPolicy, int horizontalPolicy) {
    super(item);
    setVerticalScrollBarPolicy(verticalPolicy);
    setHorizontalScrollBarPolicy(horizontalPolicy);
  }
}
