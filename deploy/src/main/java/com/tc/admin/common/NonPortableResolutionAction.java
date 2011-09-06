/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

public abstract class NonPortableResolutionAction extends AbstractResolutionAction {
  protected NonPortableWorkState fWorkState;

  public NonPortableResolutionAction(NonPortableWorkState workState) {
    fWorkState = workState;
  }

  public void actionPerformed(ActionEvent ae) {/**/
  }

  public NonPortableWorkState getWorkState() {
    return fWorkState;
  }
}
