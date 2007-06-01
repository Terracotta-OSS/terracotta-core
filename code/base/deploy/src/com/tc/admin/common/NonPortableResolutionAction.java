/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public abstract class NonPortableResolutionAction extends AbstractAction {
  protected NonPortableWorkState fWorkState;
  protected boolean              fSelected;
  protected boolean              fEnabled;

  public NonPortableResolutionAction(NonPortableWorkState workState) {
    fWorkState = workState;
    fEnabled = true;
  }

  public abstract void showControl(Object parent);
  public abstract String getText();

  public void actionPerformed(ActionEvent ae) {/**/}
  
  public NonPortableWorkState getWorkState() {
    return fWorkState;
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
