/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class ConfigurationEditorContributor
  extends MultiPageEditorActionBarContributor
{
  private IEditorPart activeEditorPart;
  private Action      sampleAction;
  
  public ConfigurationEditorContributor() {
    super();
    createActions();
  }

  protected IAction getAction(ITextEditor editor, String actionID) {
    return (editor == null ? null : editor.getAction(actionID));
  }

  public void setActivePage(IEditorPart part) {
    if(activeEditorPart == part) {
      return;
    }

    activeEditorPart = part;

    IActionBars actionBars = getActionBars();

    if(actionBars != null) {
      ITextEditor editor = (part instanceof ITextEditor) ? (ITextEditor)part : null;

      actionBars.setGlobalActionHandler(
        ActionFactory.DELETE.getId(),
        getAction(editor, ITextEditorActionConstants.DELETE));
      actionBars.setGlobalActionHandler(
        ActionFactory.UNDO.getId(),
        getAction(editor, ITextEditorActionConstants.UNDO));
      actionBars.setGlobalActionHandler(
        ActionFactory.REDO.getId(),
        getAction(editor, ITextEditorActionConstants.REDO));
      actionBars.setGlobalActionHandler(
        ActionFactory.CUT.getId(),
        getAction(editor, ITextEditorActionConstants.CUT));
      actionBars.setGlobalActionHandler(
        ActionFactory.COPY.getId(),
        getAction(editor, ITextEditorActionConstants.COPY));
      actionBars.setGlobalActionHandler(
        ActionFactory.PASTE.getId(),
        getAction(editor, ITextEditorActionConstants.PASTE));
      actionBars.setGlobalActionHandler(
        ActionFactory.SELECT_ALL.getId(),
        getAction(editor, ITextEditorActionConstants.SELECT_ALL));
      actionBars.setGlobalActionHandler(
        ActionFactory.FIND.getId(),
        getAction(editor, ITextEditorActionConstants.FIND));
      actionBars.setGlobalActionHandler(
        IDEActionFactory.BOOKMARK.getId(),
        getAction(editor, IDEActionFactory.BOOKMARK.getId()));
      actionBars.updateActionBars();
    }
  }

  private void createActions() {
    sampleAction = new Action() {
	  public void run() {
        MessageDialog.openInformation(null,
          "Tc Plug-in",
          "Sample Action Executed");
      }
    };
    sampleAction.setText("Sample Action");
    sampleAction.setToolTipText("Sample Action tool tip");
    sampleAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
	  getImageDescriptor(IDE.SharedImages.IMG_OBJS_TASK_TSK));
  }

  public void contributeToMenu(IMenuManager manager) {
    IMenuManager menu = new MenuManager("Editor &Menu");

    manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
    menu.add(sampleAction);
  }

  public void contributeToToolBar(IToolBarManager manager) {
    manager.add(new Separator());
    manager.add(sampleAction);
  }
}
