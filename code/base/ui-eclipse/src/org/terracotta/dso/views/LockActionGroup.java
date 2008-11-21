/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class LockActionGroup extends ActionGroup {
  private ConfigViewPart           fPart;
  private EditLockExpressionAction fEditExpressionAction;
  private LockLevelAction          fLevelAction;
  private AutoSynchronizedAction   fAutoSyncAction;
  private EditLockNameAction       fEditNameAction;

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
    fAutoSyncAction = new AutoSynchronizedAction(fPart);
    fEditNameAction = new EditLockNameAction(fPart);
  }

  public void setContext(ActionContext context) {
    super.setContext(context);
    fLevelAction.setContext(context);
    fEditExpressionAction.setContext(context);
    fAutoSyncAction.setContext(context);
    fEditNameAction.setContext(context);
  }

  public void fillContextMenu(IMenuManager menu) {
    if (fEditNameAction.canActionBeAdded()) {
      menu.add(fEditNameAction);
    }
    if (fEditExpressionAction.canActionBeAdded()) {
      menu.add(fEditExpressionAction);
    }
    if (fLevelAction.canActionBeAdded()) {
      menu.add(fLevelAction);
    }
    if (fAutoSyncAction.canActionBeAdded()) {
      menu.add(fAutoSyncAction);
    }
  }
}
