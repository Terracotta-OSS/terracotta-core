/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
  private LockHandler        m_lockHandler;
  private BootJarTypeAction  m_bootJarAction;
  
  public ModuleHandler() {
    super();
    
    m_adaptableAction = new AdaptableAction();
    m_excludedAction  = new ExcludedTypeAction();
    m_lockHandler     = new LockHandler();
    m_bootJarAction   = new BootJarTypeAction();
  }
  
  private static boolean isKnownConcrete(IType type) {
    try {
      return !type.isInterface();
    } catch(JavaModelException jme) {
      return false;
    }
  }

  protected IJavaElement getJavaElement(ISelection selection) {
    ICompilationUnit module = ActionUtil.findSelectedCompilationUnit(selection);   
    String           label  = "Module";
    
    if(module != null) {
      if(isKnownConcrete(module.findPrimaryType())) {
        label = "Module " + module.getElementName();
      }
      else {
        module = null;
      }
    }
    
    m_delegateAction.setText(label);
    
    return module;
  }
  
  protected void fillMenu(Menu menu) {
    if(m_element != null) {
      IType type = ((ICompilationUnit)m_element).findPrimaryType();
      
      if(type != null) {
        m_adaptableAction.setJavaElement(type);
        addMenuAction(menu, m_adaptableAction);
      
        m_excludedAction.setJavaElement(type);
        addMenuAction(menu, m_excludedAction);
      
        m_lockHandler.fillMenu(menu);

        m_bootJarAction.setType(type);
        addMenuAction(menu, m_bootJarAction);
      }
    }
  }  
}
