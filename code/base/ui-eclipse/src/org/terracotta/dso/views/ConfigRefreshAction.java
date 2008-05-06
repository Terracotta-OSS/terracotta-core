/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

public class ConfigRefreshAction extends Action {
  private ConfigViewPart fPart;

  public ConfigRefreshAction(ConfigViewPart part) {
    fPart = part;
    setText("Refresh"); 
    setToolTipText("Refresh"); 
    JavaPluginImages.setLocalImageDescriptors(this, "refresh_nav.gif");
    setActionDefinitionId("org.eclipse.ui.file.refresh");
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "refresh_action_context");
  }

  public void run() {
    fPart.refresh();
  }
}
