/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionContext;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.ui.util.SelectionUtil;

public class EditLockNameAction extends Action {
  ConfigViewPart fPart;

  EditLockNameAction(ConfigViewPart part) {
    super("Edit name...");
    fPart = part;
  }

  public void run() {
    NamedLockWrapper wrapper = (NamedLockWrapper) SelectionUtil.getSingleElement(getSelection());
    Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
    String title = "Lock name";
    String dialogMessage = "Specify lock name";
    String initialValue = wrapper.getLockName();

    InputDialog dialog = new InputDialog(shell, title, dialogMessage, initialValue, null);
    if (dialog.open() == IDialogConstants.OK_ID) {
      String lockName = dialog.getValue();

      if (lockName != null && (lockName = lockName.trim()) != null && lockName.length() > 0) {
        fPart.setLockName(lockName);
      } else if (MessageDialog.openQuestion(shell, title, "Remove lock '" + wrapper.getMethodExpression() + "'?")) {
        fPart.removeSelectedItem();
      }
    }
  }

  public void setContext(ActionContext context) {
    /**/
  }

  public boolean canActionBeAdded() {
    Object element = SelectionUtil.getSingleElement(getSelection());
    return element instanceof NamedLockWrapper;
  }

  private ISelection getSelection() {
    ISelectionProvider provider = fPart.getSite().getSelectionProvider();

    if (provider != null) { return provider.getSelection(); }

    return null;
  }
}
