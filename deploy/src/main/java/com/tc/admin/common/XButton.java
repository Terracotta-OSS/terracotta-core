/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class XButton extends JButton {
  public XButton() {
    super();
  }

  public XButton(String label) {
    super(label);
  }

  public XButton(String label, Icon icon) {
    super(label, icon);
  }

  public XButton(Icon icon) {
    super(icon);
  }

  public XButton(Action action) {
    super(action);
  }
}
