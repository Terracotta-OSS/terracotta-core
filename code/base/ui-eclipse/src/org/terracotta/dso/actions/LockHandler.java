/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.actions.ActionUtil;

/**
 * Popup action submenu that contains lock-related actions.
 * 
 * @see BaseMenuCreator
 * @see NameLockedAction
 * @see AutolockAction
 */

public class LockHandler extends BaseMenuCreator {
  private IMethod          m_method;
  private NameLockedAction m_namedLockAction;
  private AutolockAction   m_autolockAction;

  public LockHandler() {
    super();
    
    m_namedLockAction = new NameLockedAction();
    m_autolockAction  = new AutolockAction();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    return m_method = ActionUtil.findSelectedMethod(selection);
  }
  
  protected void fillMenu(Menu menu) {
    if(m_method == null) {
      return;
    }

    try {
      ConfigurationHelper config = getConfigHelper();

      if(config != null) {
        m_namedLockAction.setJavaElement(m_method);
        addMenuAction(menu, m_namedLockAction);
          
        m_autolockAction.setJavaElement(m_method);
        addMenuAction(menu, m_autolockAction);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
