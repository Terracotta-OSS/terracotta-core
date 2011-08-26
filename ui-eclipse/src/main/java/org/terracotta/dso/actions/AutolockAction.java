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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.MultiChangeSignaller;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.dialogs.AutolockDialog;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Autolock;
import com.terracottatech.config.LockLevel;

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
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    AutolockOperation operation = new AutolockOperation(m_element, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing AutolockOperation", ee);
    }
  }

  class AutolockOperation extends AbstractOperation {
    private final IJavaElement fJavaElement;
    private final boolean      fAddAutolock;
    private final String       fPattern;
    private LockLevel.Enum     fLevel;
    private boolean            fAutoSync;
    private boolean            fHaveDetails;
    private boolean            fMadeAdaptable;

    public AutolockOperation(IJavaElement javaElement, boolean addAutolock) {
      super("");
      fJavaElement = javaElement;
      fAddAutolock = addAutolock;
      fPattern = PatternHelper.getExecutionPattern(fJavaElement);
      if (fAddAutolock) {
        setLabel("Add Autolock");
      } else {
        setLabel("Remove Autolock");
      }
      fHaveDetails = false;
    }

    private void obtainDetails() {
      Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
      AutolockDialog dialog = new AutolockDialog(shell, fPattern);
      dialog.addValueListener(new UpdateEventListener() {
        public void handleUpdate(UpdateEvent e) {
          Object[] values = (Object[]) e.data;
          fAutoSync = (Boolean) values[0];
          fLevel = (LockLevel.Enum) values[1];
          fHaveDetails = true;
        }
      });
      dialog.open();
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      final ConfigurationHelper helper = getConfigHelper();

      if (fAddAutolock) {
        if (!fHaveDetails) {
          obtainDetails();
        }
        MultiChangeSignaller signaller = new MultiChangeSignaller();
        if(!helper.isAdaptable(fJavaElement)) {
          helper.ensureAdaptable(fJavaElement, signaller);
          fMadeAdaptable = true;
        }
        Autolock lock = helper.addNewAutolock(fPattern, fLevel, signaller);
        lock.setAutoSynchronized(fAutoSync);
        signaller.signal(fJavaElement.getJavaProject().getProject());
      } else {
        helper.ensureNotAutolocked(fJavaElement);
        if(fMadeAdaptable) {
          helper.ensureNotAdaptable(fJavaElement);
        }
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      final ConfigurationHelper helper = getConfigHelper();

      if (fAddAutolock) {
        helper.ensureNotAutolocked(fJavaElement);
        if(fMadeAdaptable) {
          helper.ensureNotAdaptable(fJavaElement);
        }
      } else {
        if (!fHaveDetails) {
          obtainDetails();
        }
        MultiChangeSignaller signaller = new MultiChangeSignaller();
        if(!helper.isAdaptable(fJavaElement)) {
          helper.ensureAdaptable(fJavaElement, signaller);
          fMadeAdaptable = true;
        }
        Autolock lock = helper.addNewAutolock(fPattern, fLevel, signaller);
        lock.setAutoSynchronized(fAutoSync);
        signaller.signal(fJavaElement.getJavaProject().getProject());
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
