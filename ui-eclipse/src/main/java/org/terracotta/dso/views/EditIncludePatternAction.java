/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

public class EditIncludePatternAction extends Action {
  ConfigViewPart fPart;
  
  EditIncludePatternAction(ConfigViewPart part) {
    super("Edit expression...");
    fPart = part;
  }
  
  public void run() {
    IncludeWrapper wrapper = (IncludeWrapper)SelectionUtil.getSingleElement(getSelection());
    Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
    String title = "Include expression";
    String dialogMessage = "Specify include expression";
    String initialValue = wrapper.toString();
    
    InputDialog dialog = new InputDialog(shell, title, dialogMessage, initialValue, null);
    if(dialog.open() == IDialogConstants.OK_ID) {
      String expr = dialog.getValue();
      
      if(expr != null && (expr = expr.trim()) != null && expr.length() > 0) {
        fPart.setIncludeExpression(expr);
      } else if(MessageDialog.openQuestion(shell, title, "Remove include '"+wrapper.getClassExpression()+"'?")) {
        fPart.removeSelectedItem();
      }
    }
  }

  public void setContext(ActionContext context) {
    /**/
  }

  public boolean canActionBeAdded() {
    Object element = SelectionUtil.getSingleElement(getSelection());
    return element instanceof IncludeWrapper;
  }
  
  private ISelection getSelection() {
    ISelectionProvider provider = fPart.getSite().getSelectionProvider();

    if(provider != null) {
      return provider.getSelection();
    }

    return null;
  }
}
