/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IField;
import org.terracotta.dso.ConfigurationHelper;

/**
 * Marks the currently selected field as being a shared root.
 * Creates a default root name based on the simple name of the
 * field.
 * 
 * @see org.eclipse.jdt.core.IField
 * @see BaseAction
 * @see org.terracotta.dso.ConfigurationHelper.isRoot
 * @see org.terracotta.dso.ConfigurationHelper.ensureRoot
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotRoot
 */

public class RootFieldAction extends BaseAction {
  private IField m_field;
  
  public RootFieldAction() {
    super("Shared root", AS_CHECK_BOX);
  }
  
  public void setField(IField field) {
    setJavaElement(m_field = field);
    setChecked(getConfigHelper().isRoot(m_field));
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
          
    if(isChecked()) {
      helper.ensureRoot(m_field);
    }
    else {
      helper.ensureNotRoot(m_field);
    }

    inspectCompilationUnit();
  }
}
