/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IField;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;

/**
 * Marks the currently selected field as being a transient field, that is,
 * one that is not allowed to be part of a shared root hierarchy.
 * 
 * @see org.eclipse.jdt.core.IField
 * @see BaseAction
 * @see org.terracotta.dso.ConfigurationHelper.isTransient
 * @see org.terracotta.dso.ConfigurationHelper.ensureTransient
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotTransient
 */

public class TransientFieldAction extends BaseAction {
  private IField m_field;
  
  public TransientFieldAction() {
    super("Transient field", AS_CHECK_BOX);
  }
  
  public void setField(IField field) {
    setJavaElement(m_field = field);
    setChecked(getConfigHelper().isTransient(m_field));
  }
  
  public void performAction() {
    ConfigurationHelper helper = getConfigHelper();
          
    if(isChecked()) {
      helper.ensureTransient(m_field);
    }
    else {
      helper.ensureNotTransient(m_field);
    }

    TcPlugin            plugin = TcPlugin.getDefault();
    ConfigurationEditor editor = plugin.getConfigurationEditor(getProject());

    if(editor != null) {
      editor.modelChanged();
    }

    inspectCompilationUnit();
  }
}
