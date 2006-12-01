/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.TcPlugin;

/**
 * Start the named server.
 */

public class StartServerAction extends BaseAction {
  private String m_name;
	
  public StartServerAction(IJavaProject javaProject, String name) {
    super(name);
    setJavaElement(javaProject);
    m_name = name;
  }

  public void performAction() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    
    if(!workbench.saveAllEditors(true)) {
      return;
    }
    
    TcPlugin plugin  = TcPlugin.getDefault();
    IProject project = ((IJavaProject)getJavaElement()).getProject();
    
    try {
      if(!plugin.continueWithConfigProblems(project)) {
        return;
      }
    } catch(CoreException ce) {
      Shell shell = new Shell();
      
      MessageDialog.openInformation(
        shell,
        "Terracotta",
        "Error starting Terracotta Server:\n" +
        ActionUtil.getStatusMessages(ce));
    }
    
    Display                display   = workbench.getDisplay();
    final IWorkbenchWindow window    = workbench.getActiveWorkbenchWindow();
    
    display.asyncExec(new Runnable () {
      public void run() {
        try {
          IJavaProject  javaProject = (IJavaProject)getJavaElement();
          ServerTracker tracker     = ServerTracker.getDefault();
          
          tracker.startServer(javaProject, m_name);
          ((ApplicationWindow)window).setStatus("Terracotta Server, "+m_name+", Started.");
        }
        catch(CoreException e) {
          Shell shell = new Shell();
          
          MessageDialog.openInformation(
            shell,
            "Terracotta",
            "Error starting Terracotta Server:\n" +
            ActionUtil.getStatusMessages(e));
        }
      }
    });
  }
}
