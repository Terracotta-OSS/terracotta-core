/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.ApplicationEventContext;

import java.util.Arrays;

public abstract class AbstractWorkState {
  private AbstractResolutionAction[]              fResolutionActions;

  private static final AbstractResolutionAction[] EMPTY_RESOLUTION_ACTIONS = {};

  public void setActions(AbstractResolutionAction[] actions) {
    fResolutionActions = Arrays.asList(actions).toArray(EMPTY_RESOLUTION_ACTIONS);
  }

  public AbstractResolutionAction[] getActions() {
    if(fResolutionActions != null) {
      return Arrays.asList(fResolutionActions).toArray(EMPTY_RESOLUTION_ACTIONS);
    }
    return null;
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
