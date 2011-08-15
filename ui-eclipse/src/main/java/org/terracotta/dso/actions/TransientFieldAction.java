/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IField;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Marks the currently selected field as being a transient field, that is, one that is not allowed to be part of a
 * shared root hierarchy.
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
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    TransientFieldOperation operation = new TransientFieldOperation(m_field, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing TransientFieldOperation", ee);
    }
  }

  class TransientFieldOperation extends AbstractOperation {
    private final IField  fField;
    private final boolean fMakeTransient;

    public TransientFieldOperation(IField field, boolean makeTransient) {
      super("");
      fField = field;
      fMakeTransient = makeTransient;
      if (fMakeTransient) {
        setLabel("Add Transient Field");
      } else {
        setLabel("Remove Transient Field");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeTransient) {
        helper.ensureTransient(fField);
      } else {
        helper.ensureNotTransient(fField);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeTransient) {
        helper.ensureNotTransient(fField);
      } else {
        helper.ensureTransient(fField);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
