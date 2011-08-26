/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;

public class ShowConfigEditorAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate,
             IJavaLaunchConfigurationConstants,
             IProjectAction
{
  private IJavaProject m_javaProject;
  private IAction      m_action;
 
  public ShowConfigEditorAction() {
    super("Open config editor");
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public ShowConfigEditorAction(IJavaProject javaProject) {
    super("Open config editor");
    m_javaProject = javaProject;
  }
  
  public void run(IAction action) {
    TcPlugin plugin = TcPlugin.getDefault(); 
    try {
      plugin.openConfigurationEditor(m_javaProject.getProject());
    } catch(PartInitException pie) {
      plugin.openError("Unable to open config editor", pie);
    }
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
