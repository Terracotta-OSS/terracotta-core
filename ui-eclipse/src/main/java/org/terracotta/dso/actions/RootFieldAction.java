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
 * Marks the currently selected field as being a shared root. Creates a default root name based on the simple name of
 * the field.
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
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    RootOperation operation = new RootOperation(m_field, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing RootOperation", ee);
    }
  }

  class RootOperation extends AbstractOperation {
    private final IField  fField;
    private final boolean fMakeRoot;

    public RootOperation(IField field, boolean makeRoot) {
      super("");
      fField = field;
      fMakeRoot = makeRoot;
      if (fMakeRoot) {
        setLabel("Add Shared Root");
      } else {
        setLabel("Remove Shared Root");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeRoot) {
        helper.ensureRoot(fField);
      } else {
        helper.ensureNotRoot(fField);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeRoot) {
        helper.ensureNotRoot(fField);
      } else {
        helper.ensureRoot(fField);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }

}
