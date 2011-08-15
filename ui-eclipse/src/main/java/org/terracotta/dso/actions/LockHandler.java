/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;
import org.terracotta.dso.ConfigurationHelper;

/**
 * Popup action submenu that contains lock-related actions.
 * 
 * @see BaseMenuCreator
 * @see NameLockedAction
 * @see AutolockAction
 */

public class LockHandler extends BaseMenuCreator {
  private NameLockedAction m_namedLockAction;
  private AutolockAction   m_autolockAction;

  public LockHandler() {
    super();
    
    m_namedLockAction = new NameLockedAction();
    m_autolockAction  = new AutolockAction();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    return m_element = ActionUtil.findSelectedJavaElement(selection);
  }
  
  protected void fillMenu(Menu menu) {
    if(m_element == null) {
      return;
    }

    int elementType = m_element.getElementType();
    if(elementType != IJavaElement.METHOD &&
       elementType != IJavaElement.TYPE &&
       elementType != IJavaElement.PACKAGE_FRAGMENT &&
       elementType != IJavaElement.COMPILATION_UNIT) {
      return;
    }
    
    try {
      ConfigurationHelper config = getConfigHelper();

      if(config != null) {
        m_namedLockAction.setJavaElement(m_element);
        addMenuAction(menu, m_namedLockAction);
          
        m_autolockAction.setJavaElement(m_element);
        addMenuAction(menu, m_autolockAction);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
