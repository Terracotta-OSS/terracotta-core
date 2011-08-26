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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

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
    if (!(element instanceof IType || element instanceof IPackageFragment || element instanceof IJavaProject)) { throw new IllegalArgumentException(
                                                                                                                                                    "Java element must be IType, IPackageFragment, or IJavaProject"); }
    super.setJavaElement(element);

    if (element instanceof IType) {
      IType type = (IType) element;
      if (getConfigHelper().isInstrumentationNotNeeded(type)) {
        setEnabled(false);
        setChecked(false);
      } else {
        boolean isBootClass = TcPlugin.getDefault().isBootClass(type.getJavaProject().getProject(), type);

        setEnabled(!isBootClass);
        setChecked(isBootClass || getConfigHelper().isAdaptable(type));
      }
    } else {
      setChecked(getConfigHelper().isAdaptable(element));
    }
  }

  public void performAction() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    AdaptableOperation operation = new AdaptableOperation(m_element, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing AdaptableOperation", ee);
    }
  }

  class AdaptableOperation extends AbstractOperation {
    private final IJavaElement fJavaElement;
    private final boolean      fInstrument;

    public AdaptableOperation(IJavaElement javaElement, boolean instrument) {
      super("");
      fJavaElement = javaElement;
      fInstrument = instrument;
      if (fInstrument) {
        setLabel("Add Instrumented Type");
      } else {
        setLabel("Remove Instrumented Type");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fInstrument) {
        helper.ensureAdaptable(fJavaElement);
      } else {
        helper.ensureNotAdaptable(fJavaElement);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fInstrument) {
        helper.ensureNotAdaptable(fJavaElement);
      } else {
        helper.ensureAdaptable(fJavaElement);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }

}
