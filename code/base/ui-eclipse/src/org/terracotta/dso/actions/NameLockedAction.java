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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.dialogs.NamedLockDialog;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.LockLevel;

/**
 * Marks the currently selected method as being name-locked. Creates a default name based on the simple name of the
 * method and sets the lock-type to WRITE.
 * 
 * @see org.eclipse.jdt.core.IMethod
 * @see BaseAction
 * @see org.terracotta.dso.ConfigurationHelper.isNameLocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNameLocked
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotNameLocked
 */

public class NameLockedAction extends BaseAction {
  public NameLockedAction() {
    super("Name Locked", AS_CHECK_BOX);
  }

  public void setJavaElement(IJavaElement element) {
    super.setJavaElement(element);
    setChecked(getConfigHelper().isNameLocked(element));
  }

  public void performAction(Event event) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    NamedLockOperation operation = new NamedLockOperation(m_element, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing NamedLockOperation", ee);
    }
  }

  class NamedLockOperation extends AbstractOperation {
    private final IJavaElement fJavaElement;
    private final boolean      fAddNamedLock;
    private final String       fPattern;
    private String             fName;
    private LockLevel.Enum     fLevel;
    private boolean            fHaveDetails;

    public NamedLockOperation(IJavaElement javaElement, boolean addNamedLock) {
      super("");
      fJavaElement = javaElement;
      fAddNamedLock = addNamedLock;
      fPattern = PatternHelper.getExecutionPattern(fJavaElement);
      if (fAddNamedLock) {
        setLabel("Add Named Lock");
      } else {
        setLabel("Remove Named Lock");
      }
      fHaveDetails = false;
    }

    private int obtainDetails() {
      Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
      NamedLockDialog dialog = new NamedLockDialog(shell, fPattern);
      dialog.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent e) {
          Object[] values = (Object[]) e.data;
          fName = (String) values[0];
          fLevel = (LockLevel.Enum) values[1];
          fHaveDetails = true;
        }
      });
      return dialog.open();
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      final ConfigurationHelper helper = getConfigHelper();

      if (fAddNamedLock) {
        if (!fHaveDetails) {
          if(obtainDetails() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
          }
        }
        helper.ensureNameLocked(fJavaElement, fName, fLevel);
      } else {
        helper.ensureNotNameLocked(fJavaElement);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      final ConfigurationHelper helper = getConfigHelper();

      if (fAddNamedLock) {
        helper.ensureNotNameLocked(fJavaElement);
      } else {
        if (!fHaveDetails) {
          if(obtainDetails() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
          }
        }
        helper.ensureNameLocked(fJavaElement, fName, fLevel);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
