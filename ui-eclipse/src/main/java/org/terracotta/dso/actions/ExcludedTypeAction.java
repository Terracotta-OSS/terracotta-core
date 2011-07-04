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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

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

    if (element instanceof IType) {
      IType type = (IType) element;
      boolean isBootClass = TcPlugin.getDefault().isBootClass(type.getJavaProject().getProject(), type);

      setEnabled(!isBootClass);
      setChecked(!isBootClass && getConfigHelper().isExcluded(type));
    } else {
      setChecked(getConfigHelper().isExcluded(element));
    }
  }

  public void performAction() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    ExcludeOperation operation = new ExcludeOperation(m_element, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing ExcludeOperation", ee);
    }
  }

  class ExcludeOperation extends AbstractOperation {
    private final IJavaElement fJavaElement;
    private final boolean      fAddExclude;

    public ExcludeOperation(IJavaElement javaElement, boolean addExclude) {
      super("");
      fJavaElement = javaElement;
      fAddExclude = addExclude;
      if (fAddExclude) {
        setLabel("Add Excluded Type");
      } else {
        setLabel("Remove Excluded Type");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fAddExclude) {
        helper.ensureExcluded(fJavaElement);
      } else {
        helper.ensureNotExcluded(fJavaElement);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fAddExclude) {
        helper.ensureNotExcluded(fJavaElement);
      } else {
        helper.ensureExcluded(fJavaElement);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
