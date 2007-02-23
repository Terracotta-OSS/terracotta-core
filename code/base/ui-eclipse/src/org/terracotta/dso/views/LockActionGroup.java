/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class LockActionGroup extends ActionGroup {
  private ConfigViewPart fPart;
  private EditLockExpressionAction fEditExpressionAction;
  private LockLevelAction fLevelAction;
  
  LockActionGroup(ConfigViewPart part) {
    fPart = part;
    makeActions();
  }

  ConfigViewPart getPart() {
    return fPart;
  }
  
  private void makeActions() {
    fLevelAction = new LockLevelAction(fPart);
    fEditExpressionAction = new EditLockExpressionAction(fPart);
  }
  
  public void setContext(ActionContext context) {
    super.setContext(context);
    fLevelAction.setContext(context);
    fEditExpressionAction.setContext(context);
  }

  public void fillContextMenu(IMenuManager menu) {
    if(fEditExpressionAction.canActionBeAdded()) {
      menu.add(fEditExpressionAction);
    }
    if(fLevelAction.canActionBeAdded()) {
      menu.add(fLevelAction);
    }
  }
}

