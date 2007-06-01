/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IMethod;
import org.terracotta.dso.ConfigurationHelper;

/**
 * Marks the currently selected IMethod as being a distribute method.
 * 
 * @see org.eclipse.jdt.core.IMethod
 * @see org.terracotta.dso.ConfigurationHelper.isDistributedMethod
 * @see org.terracotta.dso.ConfigurationHelper.ensureDistributedMethod
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotDistributedMethod
 */

public class DistributedMethodAction extends BaseAction {
  private IMethod m_method;
  
  public DistributedMethodAction() {
    super("Distributed method", AS_CHECK_BOX);
  }
  
  public void setMethod(IMethod method) {
    setJavaElement(m_method = method);
    setChecked(getConfigHelper().isDistributedMethod(method));
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
    
    if(isChecked()) {
      helper.ensureDistributedMethod(m_method);
    }
    else {
      helper.ensureLocalMethod(m_method);
    }

    inspectCompilationUnit();
  }
}
