/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import org.terracotta.dso.TcPlugin;

public class OpenConfigurationAction implements IObjectActionDelegate {
  private IJavaProject m_currentProject;
  
  public OpenConfigurationAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {/**/}

  public void run(IAction action) {
    try {
      IProject project = m_currentProject.getProject();
      
      TcPlugin.getDefault().openConfigurationEditor(project);
    } catch(PartInitException pie) {
      pie.printStackTrace();
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_currentProject = ActionUtil.locateSelectedJavaProject(selection);
  }
}
