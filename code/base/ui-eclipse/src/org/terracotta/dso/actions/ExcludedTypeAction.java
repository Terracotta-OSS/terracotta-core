/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;

/**
 * Marks the currently selected IType as being excluded from instrumentation.
 * 
 * @see org.eclipse.jdt.core.IType
 * @see org.terracotta.dso.ConfigurationHelper.isExcluded
 * @see org.terracotta.dso.ConfigurationHelper.ensureExcluded
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotExcluded
 */

public class ExcludedTypeAction extends BaseAction {
  public ExcludedTypeAction() {
    super("Excluded", AS_CHECK_BOX);
  }
  
  public void setJavaElement(IJavaElement element) {
    super.setJavaElement(element);
    
    if(element instanceof IType) {
      IType   type        = (IType)element;
      boolean isBootClass = TcPlugin.getDefault().isBootClass(type); 
      
      setEnabled(!isBootClass);
      setChecked(!isBootClass && getConfigHelper().isExcluded(type));
    }
    else {
      setChecked(getConfigHelper().isExcluded(element));
    }
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
    
    if(isChecked()) {
      helper.ensureExcluded(m_element);
    }
    else {
      helper.ensureNotExcluded(m_element);
    }

    TcPlugin            plugin = TcPlugin.getDefault();
    ConfigurationEditor editor = plugin.getConfigurationEditor(getProject());

    if(editor != null) {
      editor.modelChanged();
    }

    inspectCompilationUnit();
  }
}
