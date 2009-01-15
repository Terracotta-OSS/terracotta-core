/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.Icon;
import javax.swing.JLabel;

public class XLabel extends JLabel {
  public XLabel() {
    super("");
  }

  public XLabel(String text) {
    super(text);
  }

  public XLabel(String text, int alignment) {
    super(text);
    setHorizontalAlignment(alignment);
  }

  public XLabel(Icon icon) {
    super("");
    setIcon(icon);
  }

  public XLabel(String text, Icon icon) {
    this(text);
    setIcon(icon);
  }
}
