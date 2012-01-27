/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.JFileChooser;

/**
 * Java6 Update10 fixes the performance problem this class is meant to 
 * work-around.
 */
public class FastFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1541813407103968847L;

  @Override
  public void updateUI() {
    putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
    super.updateUI();
  }
}

