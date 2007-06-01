/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.terracotta.dso.ConfigurationHelper;

/**
 * Mark the currently selected IMethod as being autolocked.
 * 
 * @see org.terracotta.dso.ConfigurationHelper.isAutolocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureAutolocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotAutolocked
 */

public class AutolockAction extends BaseAction {
  public AutolockAction() {
    super("Autolock", AS_CHECK_BOX);
  }
  
  public void setJavaElement(IJavaElement element) {
    super.setJavaElement(element);
    setChecked(getConfigHelper().isAutolocked(element));
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
    
    if(isChecked()) {
      helper.ensureAutolocked(m_element);
    }
    else {
      helper.ensureNotAutolocked(m_element);
    }

    inspectCompilationUnit();
  }
}
