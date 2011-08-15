/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionContext;
import org.terracotta.ui.util.SelectionUtil;

import com.terracottatech.config.LockLevel;

public class LockLevelAction extends Action implements IMenuCreator {
  ConfigViewPart fPart;
  Action fRead;
  Action fWrite;
  Action fConcurrent;
  Action fSynchronousWrite;
  
  LockLevelAction(ConfigViewPart part) {
    super("Lock level", AS_DROP_DOWN_MENU);
    setMenuCreator(this);
    fPart = part;
    
    fRead = new Action("Read", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          LockLevelAction.this.run();
        }
      }
    };
    fWrite = new Action("Write", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          LockLevelAction.this.run();
        }
      }
    };
    fConcurrent = new Action("Concurrent", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          LockLevelAction.this.run();
        }
      }
    };
    fSynchronousWrite = new Action("Synchronous-write", AS_RADIO_BUTTON) {
      public void run() {
        if(isChecked()) {
          LockLevelAction.this.run();
        }
      }
    };
  }
  
  public void run() {
    fPart.setLockLevel(this);
  }

  LockLevel.Enum getLevel() {
    if(fRead.isChecked()) {
      return LockLevel.READ;
    } else if(fWrite.isChecked()) {
      return LockLevel.WRITE;
    } else if(fConcurrent.isChecked()){
      return LockLevel.CONCURRENT;
    } else if(fSynchronousWrite.isChecked()) {
      return LockLevel.SYNCHRONOUS_WRITE;
    }
    
    return LockLevel.WRITE;
  }
  
  public void setContext(ActionContext context) {
    Object element = SelectionUtil.getSingleElement(getSelection());

    if(element instanceof LockWrapper) {
      LockWrapper wrapper = (LockWrapper)element;
      LockLevel level = wrapper.getLevel();
      
      fRead.setChecked(level != null && level.enumValue() == LockLevel.READ);
      fWrite.setChecked(level == null || level.enumValue() == LockLevel.WRITE);
      fConcurrent.setChecked(level != null && level.enumValue() == LockLevel.CONCURRENT);
      fSynchronousWrite.setChecked(level != null && level.enumValue() == LockLevel.SYNCHRONOUS_WRITE);
    }
  }

  public boolean canActionBeAdded() {
    Object element = SelectionUtil.getSingleElement(getSelection());
    return element instanceof LockWrapper;
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
    
    item = new ActionContributionItem(fRead);
    item.fill(menu, -1);

    item = new ActionContributionItem(fWrite);
    item.fill(menu, -1);

    item = new ActionContributionItem(fConcurrent);
    item.fill(menu, -1);

    item = new ActionContributionItem(fSynchronousWrite);
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
}
