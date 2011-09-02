/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public abstract class AbstractResolutionAction extends AbstractAction {
  protected boolean fSelected;
  protected boolean fEnabled;

  public AbstractResolutionAction() {
    fEnabled = true;
  }

  public abstract void showControl(Object parent);

  public abstract String getText();

  public void actionPerformed(ActionEvent ae) {/**/
  }

  public void apply() {/**/
  }

  public void setSelected(boolean selected) {
    fSelected = selected;
  }

  public boolean isSelected() {
    return fSelected;
  }

  public void setEnabled(boolean enabled) {
    fEnabled = enabled;
  }

  public boolean isEnabled() {
    return fEnabled;
  }

  public String toString() {
    return getText();
  }
}
