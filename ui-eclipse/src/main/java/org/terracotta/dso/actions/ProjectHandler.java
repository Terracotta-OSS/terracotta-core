/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

/**
 * Popup action submenu that holds actions that are Java project-related.
 * 
 * @see org.eclipse.jdt.core.IJavaProject
 * @see BaseMenuCreator
 * @see AdaptableAction
 * @see ExcludedTypeAction
 * @see LockHandler
 */

public class ProjectHandler extends BaseMenuCreator {
  private IJavaProject       m_javaProject;
  private AdaptableAction    m_adaptableAction;
  private ExcludedTypeAction m_excludedAction;
  private LockHandler        m_lockHandler;
  
  public ProjectHandler() {
    super();
    
    m_adaptableAction = new AdaptableAction();
    m_excludedAction  = new ExcludedTypeAction();
    m_lockHandler     = new LockHandler();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    return m_javaProject = ActionUtil.findSelectedJavaProject(selection);    
  }
  
  protected void fillMenu(Menu menu) {
    if(m_javaProject != null) {
      m_adaptableAction.setJavaElement(m_javaProject);
      addMenuAction(menu, m_adaptableAction);
      
      m_excludedAction.setJavaElement(m_javaProject);
      addMenuAction(menu, m_excludedAction);
      
      m_lockHandler.fillMenu(menu);
    }
  }  
}
