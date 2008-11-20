/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

/**
 * Popup action submenu that contains module-related actions.
 * 
 * @see org.eclipse.jdt.core.ICompilationUnit
 * @see BaseMenuCreator
 * @see AdaptableAction
 * @see ExcludedTypeAction
 * @see BootJarTypeAction
 */

public class ModuleHandler extends BaseMenuCreator {
  private AdaptableAction    m_adaptableAction;
  private ExcludedTypeAction m_excludedAction;
  private BootJarTypeAction  m_bootJarAction;

  public ModuleHandler() {
    super();

    m_adaptableAction = new AdaptableAction();
    m_excludedAction = new ExcludedTypeAction();
    m_bootJarAction = new BootJarTypeAction();
  }

  protected IJavaElement getJavaElement(ISelection selection) {
    ICompilationUnit module = ActionUtil.findSelectedCompilationUnit(selection);
    String label = "Module";

    if (module != null) {
      label = "Module " + module.getElementName();
    }

    m_delegateAction.setText(label);

    return module;
  }

  protected void fillMenu(Menu menu) {
    if (m_element != null) {
      IType type = ((ICompilationUnit) m_element).findPrimaryType();

      if (type != null) {
        m_adaptableAction.setJavaElement(type);
        addMenuAction(menu, m_adaptableAction);

        m_excludedAction.setJavaElement(type);
        addMenuAction(menu, m_excludedAction);

        m_bootJarAction.setType(type, menu);
//        addMenuAction(menu, m_bootJarAction);
      }
    }
  }
}
