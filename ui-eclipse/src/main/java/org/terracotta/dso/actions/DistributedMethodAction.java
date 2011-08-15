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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

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
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    DistributedMethodOperation operation = new DistributedMethodOperation(m_method, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing DistributedMethodOperation", ee);
    }
  }

  class DistributedMethodOperation extends AbstractOperation {
    private final IMethod fMethod;
    private final boolean fMakeDistributed;

    public DistributedMethodOperation(IMethod method, boolean makeDistributed) {
      super("");
      fMethod = method;
      fMakeDistributed = makeDistributed;
      if (fMakeDistributed) {
        setLabel("Add Distibuted Method");
      } else {
        setLabel("Remove Distibuted Method");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeDistributed) {
        helper.ensureDistributedMethod(fMethod);
      } else {
        helper.ensureLocalMethod(fMethod);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeDistributed) {
        helper.ensureLocalMethod(fMethod);
      } else {
        helper.ensureDistributedMethod(fMethod);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
