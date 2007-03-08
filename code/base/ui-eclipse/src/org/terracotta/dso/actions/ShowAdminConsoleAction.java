/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.terracotta.dso.ClasspathProvider;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;

import com.tc.admin.common.InputStreamDrainer;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;

public class ShowAdminConsoleAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate,
             IJavaLaunchConfigurationConstants,
             IProjectAction
{
  private IJavaProject m_javaProject;
  private IAction      m_action;
  
  public ShowAdminConsoleAction() {
    super("Show AdminConsole...");
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public ShowAdminConsoleAction(IJavaProject javaProject) {
    super("Show AdminConsole...");
    m_javaProject = javaProject;
  }
  
  public void run(IAction action) {
    String[] cmdarray = {
      getJavaCmd().getAbsolutePath(),
      "-Dtc.install-root=" + TcPlugin.getDefault().getLocation().toOSString(),
      "-cp", getClasspath(),
      "com.tc.admin.AdminClient"
    };

    Process p = exec(cmdarray, null, new File(System.getProperty("user.dir")));

    new InputStreamDrainer(p.getErrorStream()).start();
    new InputStreamDrainer(p.getInputStream()).start();
    IOUtils.closeQuietly(p.getOutputStream());
  }

  private String getClasspath() {
    TcPlugin plugin = TcPlugin.getDefault();
    IPath tcJarPath = plugin.getLibDirPath().append("tc.jar");

    if(tcJarPath.toFile().exists()){
      return tcJarPath.toOSString();
    } else {
      return ClasspathProvider.makeDevClasspath();
    }
  }
  
  private File getJavaCmd() {
    File javaBin = new File(System.getProperty("java.home"), "bin");
    return new File(javaBin, "java" + (Os.isWindows() ? ".exe" : ""));
  }
  
  protected Process exec(String[] cmdarray, String[] envp, File dir) {
    try {
      return Runtime.getRuntime().exec(cmdarray, envp, dir);
    } catch(IOException ioe) {
      TcPlugin.getDefault().openError("Unable to show AdminConsole", ioe);
    }
    
    return null;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_action = action;
    
    if(m_javaProject == null || selection instanceof IStructuredSelection) {
      update(ActionUtil.locateSelectedJavaProject(selection));
    }
    else {
      action.setEnabled(true);
    }
  }

  private void update(IJavaProject javaProject) {
    if(javaProject != null) {
      try {
        if(javaProject.getProject().hasNature(ProjectNature.NATURE_ID)) {
          m_javaProject = javaProject;
        }
        else {
          m_javaProject = null;
        }
      } catch(CoreException ce) {/**/}
    }
    else {
      m_javaProject = null;
    }
    
    m_action.setEnabled(m_javaProject != null);
  }
  
  public void update(IProject project) {
    update(ActionUtil.findJavaProject(project));
  }
  
  public void dispose() {
    /**/
  }

  public void init(IWorkbenchWindow window) {
    /**/
  }
}
