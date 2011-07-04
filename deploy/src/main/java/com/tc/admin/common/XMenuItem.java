/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

public class XMenuItem extends JMenuItem {
  public XMenuItem() {
    super();
  }

  public XMenuItem(String label) {
    super(label);
  }

  public XMenuItem(String label, Icon icon) {
    super(label, icon);
  }

  public XMenuItem(Action action) {
    super(action);
  }

}
