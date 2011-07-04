/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class IncludeActionGroup extends ActionGroup {
  private ConfigViewPart fPart;
  private EditIncludePatternAction fEditPatternAction;
  private HonorTransientAction fHonorTransientAction;
  private OnLoadAction fOnLoadAction;
  
  IncludeActionGroup(ConfigViewPart part) {
    fPart = part;
    makeActions();
  }

  ConfigViewPart getPart() {
    return fPart;
  }
  
  private void makeActions() {
    fHonorTransientAction = new HonorTransientAction(fPart);
    fOnLoadAction = new OnLoadAction(fPart);
    fEditPatternAction = new EditIncludePatternAction(fPart);
  }
  
  public void setContext(ActionContext context) {
    super.setContext(context);
    fHonorTransientAction.setContext(context);
    fOnLoadAction.setContext(context);
    fEditPatternAction.setContext(context);
  }

  public void fillContextMenu(IMenuManager menu) {
    if(fEditPatternAction.canActionBeAdded()) {
      menu.add(fEditPatternAction);
    }
    if(fHonorTransientAction.canActionBeAdded()) {
      menu.add(fHonorTransientAction);
    }
    if(fOnLoadAction.canActionBeAdded()) {
      menu.add(fOnLoadAction);
    }
  }
}

