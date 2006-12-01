/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.actions.ActionUtil;

/**
 * Shutdown the currently running server.
 */

public class StopServerAction implements IObjectActionDelegate {
  private IJavaProject m_currentProject;
	
  public StopServerAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {/**/}

  public void run(IAction action) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    try {
      ServerTracker.getDefault().stopServer(m_currentProject);
      ((ApplicationWindow)window).setStatus("Terracotta Server Stopped.");
    }
    catch(Exception e) {
      Shell shell = new Shell();
      MessageDialog.openInformation(
        shell,
        "Terracotta",
        "Cannot stop Terracotta Server:\n" +
        ActionUtil.getStatusMessages(e));
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_currentProject = ActionUtil.findSelectedJavaProject(selection);
  }
}
