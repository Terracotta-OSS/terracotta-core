/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ServerTracker;

import java.lang.reflect.InvocationTargetException;

/**
 * Shutdown the currently running server.
 */

public class StopServerAction implements IObjectActionDelegate, IRunnableWithProgress {
  private IJavaProject m_currentProject;
	
  public StopServerAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {/**/}

  public void run(IAction action) {
    try {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      window.run(true, true, this);
    }
    catch(Exception e) {
      Shell shell = new Shell();
      MessageDialog.openInformation(
        shell,
        "Terracotta",
        "Cannot stop Terracotta Server instance:\n" +
        ActionUtil.getStatusMessages(e));
    }
  }
  
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    if(monitor != null && monitor.isCanceled()) throw new InterruptedException("Canceled");
    ServerTracker.getDefault().stopServer(m_currentProject, monitor);
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    m_currentProject = ActionUtil.findSelectedJavaProject(selection);
  }
}
