/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.terracotta.dso.TcPlugin;

/**
 * Revert the current Terracotta project back to a plain Java Project.
 */

public class RemoveTerracottaNatureAction implements IObjectActionDelegate {
  private IJavaProject m_currentProject;
    
  public RemoveTerracottaNatureAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {/**/}

  public void run(IAction action) {
    TcPlugin.getDefault().removeTerracottaNature(m_currentProject);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_currentProject = ActionUtil.findSelectedJavaProject(selection);
  }
}
