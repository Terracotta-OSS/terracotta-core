/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.MenuItem;

import javax.swing.Icon;

public class XMenuItem extends MenuItem {
  public XMenuItem() {
    super();
  }

  public XMenuItem(String label) {
    super(label);
  }
  
  public XMenuItem(String label, Icon icon) {
    super(label, icon);
  }
  
}
