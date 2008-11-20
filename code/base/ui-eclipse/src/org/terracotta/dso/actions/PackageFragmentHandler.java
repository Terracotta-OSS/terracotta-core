/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Menu;

import org.terracotta.dso.ProjectNature;

/**
 * Popup action submenu that holds actions that are package fragment-related.
 * 
 * @see org.eclipse.jdt.core.IType
 * @see BaseMenuCreator
 * @see AdaptableAction
 * @see ExcludedTypeAction
 * @see BootJarTypeAction
 */

public class PackageFragmentHandler extends BaseMenuCreator {
  private IPackageFragment   m_fragment;
  private AdaptableAction    m_adaptableAction;
  private ExcludedTypeAction m_excludedAction;
  
  public PackageFragmentHandler() {
    super();
    
    m_adaptableAction = new AdaptableAction();
    m_excludedAction  = new ExcludedTypeAction();
  }
  
  protected IJavaElement getJavaElement(ISelection selection) {
    IPackageFragment fragment = ActionUtil.findSelectedPackageFragment(selection);
    String label = "Package";
    
    m_fragment = null;
    
    if(fragment != null) {
      IProject project = fragment.getJavaProject().getProject();
      
      try {
        if(project.hasNature(ProjectNature.NATURE_ID)) {
          m_fragment = fragment;
          label = "Package " + fragment.getElementName();
        }
      } catch(Exception e) {/**/}
    }
    
    m_delegateAction.setText(label);

    return m_fragment;
  }
  
  protected void fillMenu(Menu menu) {
    if(m_fragment != null) {
      m_adaptableAction.setJavaElement(m_fragment);
      addMenuAction(menu, m_adaptableAction);
      
      m_excludedAction.setJavaElement(m_fragment);
      addMenuAction(menu, m_excludedAction);
    }
  }  
}
