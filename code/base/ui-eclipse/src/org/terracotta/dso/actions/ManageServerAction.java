/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.TcPlugin;

/**
 * Manage the named server.
 */

public class ManageServerAction extends BaseAction {
  private String                 m_name;
  private static ImageDescriptor m_startImage;
  private static ImageDescriptor m_stopImage;
  
  static {
    String pluginId  = TcPlugin.getPluginId();
    String startPath = "/images/eclipse/start.gif";
    String stopPath  = "/images/eclipse/stop.gif";
    
    m_startImage = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, startPath);
    m_stopImage  = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, stopPath);
  }
	
  public ManageServerAction(IJavaProject javaProject, String name) {
    super(name);
    setJavaElement(javaProject);
    m_name = name;
    
    ServerTracker serverTracker = ServerTracker.getDefault(); 
    ImageDescriptor imageDesc;
    imageDesc = serverTracker.isRunning(javaProject, name) ? m_stopImage : m_startImage;
    setImageDescriptor(imageDesc);
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
    
    Display                display = workbench.getDisplay();
    final IWorkbenchWindow window  = workbench.getActiveWorkbenchWindow();
    
    display.asyncExec(new Runnable () {
      public void run() {
        try {
          IJavaProject  javaProject = (IJavaProject)getJavaElement();
          ServerTracker tracker     = ServerTracker.getDefault();
          
          if(tracker.isRunning(javaProject, m_name)) {
            tracker.stopServer(javaProject, m_name);
          } else {
            tracker.startServer(javaProject, m_name);
            ((ApplicationWindow)window).setStatus("Terracotta Server, "+m_name+", Started.");
          }
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
