/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SelectionUtil;

import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.util.List;

public class DeleteAction extends Action {
  private ConfigViewPart fPart;

  public DeleteAction(ConfigViewPart part) {
    fPart = part;
    setText("Delete");
    setToolTipText("Delete");

    String iconPath = "images/eclipse/delete_edit.gif";
    setImageDescriptor(TcPlugin.getImageDescriptor(iconPath));
  }

  public boolean canActionBeAdded() {
    List list = SelectionUtil.toList(getSelection());

    if (list != null && list.size() > 0) {
      for (int i = 0; i < list.size(); i++) {
        Object element = list.get(i);

        if (element instanceof RootWrapper || element instanceof NamedLockWrapper || element instanceof AutolockWrapper
            || element instanceof BootClassWrapper || element instanceof TransientFieldWrapper
            || element instanceof DistributedMethodWrapper || element instanceof IncludeWrapper
            || element instanceof ExcludeWrapper) {
          continue;
        } else {
          return false;
        }
      }
    }

    return list != null;
  }

  private ISelection getSelection() {
    ISelectionProvider provider = fPart.getSite().getSelectionProvider();

    if (provider != null) { return provider.getSelection(); }

    return null;
  }

  public void run() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    DeleteOperation operation = new DeleteOperation();
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing DeleteOperation", ee);
    }
  }

  class DeleteOperation extends AbstractOperation {
    TcConfig fPreConfig;
    TcConfig fPostConfig;

    DeleteOperation() {
      super("Delete config elements");
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      fPreConfig = (TcConfig) fPart.getConfig().copy();
      fPart.removeSelectedItem();
      fPostConfig = (TcConfig) fPart.getConfig().copy();
      return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
      try {
        setConfig(fPostConfig);
      } catch (Exception e) {
        throw new ExecutionException("Redoing delete", e);
      }
      return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
      try {
        setConfig(fPreConfig);
      } catch (Exception e) {
        throw new ExecutionException("Undoing delete", e);
      }
      return Status.OK_STATUS;
    }

    private void setConfig(TcConfig config) throws Exception {
      TcPlugin plugin = TcPlugin.getDefault();
      IJavaProject javaProject = fPart.getJavaProject();
      XmlOptions xmlOpts = plugin.getXmlOptions();
      TcConfigDocument doc = TcConfigDocument.Factory.newInstance(xmlOpts);
      doc.setTcConfig(config);
      plugin.setConfigurationFromString(javaProject.getProject(), plugin.configDocumentAsString(doc));
    }
  }
}
