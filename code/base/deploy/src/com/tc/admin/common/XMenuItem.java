/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
