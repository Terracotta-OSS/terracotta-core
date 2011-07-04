/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

/**
 * Popup action submenu that contains method-related actions.
 * 
 * @see BaseMenuCreator
 * @see AutolockAction
 * @see NameLockedAction
 * @see NameLockedAction
 */

public class MethodHandler extends BaseMenuCreator {
  private AutolockAction          m_autolockAction;
  private NameLockedAction        m_namedLockAction;
  private DistributedMethodAction m_distributedAction;

  public MethodHandler() {
    super();
    
    m_autolockAction    = new AutolockAction();
    m_namedLockAction   = new NameLockedAction();
    m_distributedAction = new DistributedMethodAction();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    IJavaElement elem  = null;
    String       label = "Methods";
    
    if((elem = ActionUtil.findSelectedJavaElement(selection)) != null) {
      if(elem.getElementType() == IJavaElement.METHOD) {
        label = "Method " + elem.getElementName();
      }
    }

    m_delegateAction.setText(label);
    
    return elem;
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

    m_autolockAction.setJavaElement(m_element);
    addMenuAction(menu, m_autolockAction);

    m_namedLockAction.setJavaElement(m_element);
    addMenuAction(menu, m_namedLockAction);

    if(m_element.getElementType() == IJavaElement.METHOD) {
      m_distributedAction.setMethod((IMethod)m_element);
      addMenuAction(menu, m_distributedAction);
    }
  }
}
