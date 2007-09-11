/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.ApplicationEventContext;

public abstract class AbstractWorkState {
  private AbstractResolutionAction[] fResolutionActions;

  public void setActions(AbstractResolutionAction[] actions) {
    fResolutionActions = actions;
  }

  public AbstractResolutionAction[] getActions() {
    return fResolutionActions;
  }

  public boolean hasSelectedActions() {
    if (fResolutionActions != null && fResolutionActions.length > 0) {
      for (int i = 0; i < fResolutionActions.length; i++) {
        if (fResolutionActions[i].isSelected()) { return true; }
      }
    }

    return false;
  }
  
  public abstract String summary();
  public abstract String descriptionFor(ApplicationEventContext context);
}
