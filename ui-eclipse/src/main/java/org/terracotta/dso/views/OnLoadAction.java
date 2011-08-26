/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionContext;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.ui.util.SelectionUtil;

import com.terracottatech.config.OnLoad;

public class OnLoadAction extends Action implements IMenuCreator {
  ConfigViewPart fPart;
  Action fNoop;
  Action fExecute;
  Action fMethod;
  Action fEdit;
  
  OnLoadAction(ConfigViewPart part) {
    super("On load", AS_DROP_DOWN_MENU);
    setMenuCreator(this);
    fPart = part;
    
    fNoop = new Action("Do nothing", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          fPart.setOnLoad(OnLoadAction.this, "");
        }
      }
    };
    fExecute = new Action("Execute code", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          OnLoadAction.this.run();
          fEdit.run();
        }
      }
    };
    fMethod = new Action("Call method", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          OnLoadAction.this.run();
          fEdit.run();
        }
      }
    };
    
    fEdit = new Action("Edit...") {
      public void run() {
        IncludeWrapper wrapper = (IncludeWrapper)SelectionUtil.getSingleElement(getSelection());
        boolean isExecute = isExecute();
        Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
        OnLoadDialog dialog = new OnLoadDialog(shell, wrapper, isExecute);
        try {
          if(dialog.open() == IDialogConstants.OK_ID) {
            fPart.setOnLoad(OnLoadAction.this, dialog.getResult());
          }
        } catch(Throwable t) {
          t.printStackTrace();
        }
      }
    };
  }
  
  public void run() {
    //fPart.setOnLoad(this, "");
  }

  boolean isNoop() {return fNoop.isChecked();}
  boolean isExecute() {return fExecute.isChecked();}
  boolean isMethod() {return fMethod.isChecked();}
  
  public void setContext(ActionContext context) {
    Object element = SelectionUtil.getSingleElement(getSelection());

    if(element instanceof IncludeWrapper) {
      IncludeWrapper wrapper = (IncludeWrapper)element;
      
      fNoop.setChecked(!wrapper.isSetOnLoad());
      fExecute.setChecked(wrapper.isSetOnLoadExecute());
      fMethod.setChecked(wrapper.isSetOnLoadMethod());
      
      fEdit.setEnabled(wrapper.isSetOnLoad());
    }
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

  public void dispose() {
    /**/
  }

  private void fillMenu(Menu menu) {
    ActionContributionItem item;
    
    item = new ActionContributionItem(fNoop);
    item.fill(menu, -1);

    item = new ActionContributionItem(fExecute);
    item.fill(menu, -1);

    item = new ActionContributionItem(fMethod);
    item.fill(menu, -1);

    new Separator().fill(menu, -1);
    
    item = new ActionContributionItem(fEdit);
    item.fill(menu, -1);
  }
  
  public Menu getMenu(Control parent) {
    Menu menu = new Menu(parent);
    fillMenu(menu);
    return menu;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = new Menu(parent);
    fillMenu(menu);
    return menu;
  }

  class OnLoadDialog extends MessageDialog {
    IncludeWrapper fWrapper;
    boolean fIsExecute;
    Text fText;
    String fResult;
    
    OnLoadDialog(Shell shell, IncludeWrapper wrapper, boolean isExecute) {
      super(shell, "OnLoad: "+wrapper, null,
            isExecute() ? "Execute script" : "Call method",
            MessageDialog.NONE,
            new String[] {IDialogConstants.OK_LABEL,
                          IDialogConstants.CANCEL_LABEL}, 0);
      fWrapper = wrapper;
      fIsExecute = isExecute;
    }
    
    protected Control createCustomArea(Composite parent) {
      int flags = fIsExecute ? SWT.MULTI | SWT.V_SCROLL : 0;
      fText = new Text(parent, flags | SWT.BORDER);
      GridData gd = new GridData(GridData.FILL_BOTH);
      if(fIsExecute) gd.heightHint = 4 * fText.getLineHeight();
      fText.setLayoutData(gd);
      OnLoad onload = fWrapper.ensureOnLoad();
      String text = fIsExecute ? onload.getExecute() : onload.getMethod(); 
      fText.setText(text == null ? "" : text);
      fText.selectAll();
      return fText;
    }
    
    public boolean close() {
      fResult = fText.getText().trim();
      return super.close();
    }
    
    String getResult() {
      return fResult;
    }
  }
}
