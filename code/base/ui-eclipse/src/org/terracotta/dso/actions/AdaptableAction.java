/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;

/**
 * Mark the currently selected IType as instrumented.
 */

public class AdaptableAction extends BaseAction {
  public AdaptableAction() {
    super("Instrumented", AS_CHECK_BOX);
  }
  
  /**
   * The IJavaElement must be one of IType, IPackageFragment, or IJavaProject.
   */
  public void setJavaElement(IJavaElement element) {
    if(!(element instanceof IType            ||
         element instanceof IPackageFragment ||
         element instanceof IJavaProject))
    {
      throw new IllegalArgumentException(
        "Java element must be IType, IPackageFragment, or IJavaProject");
    }
    
    super.setJavaElement(element);
    
    if(element instanceof IType) {
      IType   type        = (IType)element;
      boolean isBootClass = TcPlugin.getDefault().isBootClass(type); 

      setEnabled(!isBootClass);
      setChecked(isBootClass || getConfigHelper().isAdaptable(type));
    }
    else {
      setChecked(getConfigHelper().isAdaptable(element));
    }
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();

    if(isChecked()) {
      helper.ensureAdaptable(m_element);
    }
    else {
      helper.ensureNotAdaptable(m_element);
    }

    TcPlugin            plugin = TcPlugin.getDefault();
    ConfigurationEditor editor = plugin.getConfigurationEditor(getProject());

    if(editor != null) {
      editor.modelChanged();
    }

    inspectCompilationUnit();
  }
}
