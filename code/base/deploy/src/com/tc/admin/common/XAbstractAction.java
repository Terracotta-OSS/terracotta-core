/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Toolkit;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.Icon;

public abstract class XAbstractAction extends AbstractAction {
  protected static final int MENU_SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  public XAbstractAction() {
    super();
  }

  public XAbstractAction(String name) {
    super(name);
    setShortDescription(name);
  }

  public XAbstractAction(String name, Icon icon) {
    super(name, icon);
    setShortDescription(name);
  }

  public void setName(String name) {
    putValue(Action.NAME, name);
  }

  public String getName() {
    return (String)getValue(Action.NAME);
  }

  public void setSmallIcon(Icon icon) {
    putValue(Action.SMALL_ICON, icon);
  }

  public Icon getSmallIcon() {
    return (Icon)getValue(Action.SMALL_ICON);
  }

  public void setAccelerator(KeyStroke ks) {
    putValue(ACCELERATOR_KEY, ks);
  }

  public KeyStroke getAccelerator() {
    return (KeyStroke)getValue(ACCELERATOR_KEY);
  }

  public void setShortDescription(String description) {
    putValue(Action.SHORT_DESCRIPTION, description);
  }

  public String getShortDescription() {
    return (String)getValue(Action.SHORT_DESCRIPTION);
  }

  public void setLongDescription(String description) {
    putValue(Action.LONG_DESCRIPTION, description);
  }

  public String getLongDescription() {
    return (String)getValue(Action.LONG_DESCRIPTION);
  }

}
