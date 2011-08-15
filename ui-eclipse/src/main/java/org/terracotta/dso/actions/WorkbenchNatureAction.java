/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;

public class WorkbenchNatureAction
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate,
             IProjectAction
{
  private IJavaProject m_javaProject;
  private boolean      m_addNature;
  private IAction      m_action;
  
  public WorkbenchNatureAction() {
    super();
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public void run(IAction action) {
    TcPlugin plugin = TcPlugin.getDefault();
    
    if(m_addNature) {
      plugin.addTerracottaNature(m_javaProject);
    }
    else {
      plugin.removeTerracottaNature(m_javaProject);
    }
    
    update(m_javaProject);
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
    if((m_javaProject = javaProject) != null) {
      String text;
      
      try {
        IProject project = javaProject.getProject();
        
        if(!project.isOpen()) {
          m_action.setText("Add Terracotta Nature...");
          m_action.setEnabled(false);
          return;
        }
        
        if(project.hasNature(ProjectNature.NATURE_ID)) {
          m_addNature = false;
          text = "Remove Terracotta Nature";
        }
        else {
          m_addNature = true;
          text = "Add Terracotta Nature...";
        }
      } catch(Exception ce) {
        ce.printStackTrace();
        m_action.setEnabled(false);
        return;
      }
  
      m_action.setText(text);
      m_action.setEnabled(true);
    }
    else {
      m_action.setEnabled(false);
    }
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
