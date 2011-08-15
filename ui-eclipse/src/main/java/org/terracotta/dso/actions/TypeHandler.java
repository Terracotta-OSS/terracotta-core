/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

/**
 * Popup action submenu that holds actions that are type-related.
 * 
 * @see org.eclipse.jdt.core.IType
 * @see BaseMenuCreator
 * @see AdaptableAction
 * @see ExcludedTypeAction
 * @see BootJarTypeAction
 */

public class TypeHandler extends BaseMenuCreator {
  private AdaptableAction    m_adaptableAction;
  private ExcludedTypeAction m_excludedAction;
  private BootJarTypeAction  m_bootJarAction;

  public TypeHandler() {
    super();

    m_adaptableAction = new AdaptableAction();
    m_excludedAction = new ExcludedTypeAction();
    m_bootJarAction = new BootJarTypeAction();
  }

  protected IJavaElement getJavaElement(ISelection selection) {
    IJavaElement elem = null;
    String label = "Types";

    if ((elem = ActionUtil.findSelectedType(selection)) != null) {
      label = "Type " + elem.getElementName();
    } else if ((elem = ActionUtil.findSelectedPackageFragment(selection)) != null) {
      label = "Package " + elem.getElementName();
    }

    m_delegateAction.setText(label);

    return elem;
  }

  protected void fillMenu(Menu menu) {
    if (m_element != null) {
      m_adaptableAction.setJavaElement(m_element);
      addMenuAction(menu, m_adaptableAction);

      m_excludedAction.setJavaElement(m_element);
      addMenuAction(menu, m_excludedAction);

      if (m_element instanceof IType) {
        m_bootJarAction.setType((IType) m_element, menu);
//        addMenuAction(menu, m_bootJarAction);
      }
    }
  }
}
