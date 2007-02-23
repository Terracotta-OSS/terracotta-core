/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.terracotta.dso.TcPlugin;

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
    
    if(list != null && list.size() > 0) {
      for(int i = 0; i < list.size(); i++) {
        Object element = list.get(i);
        
        if(element instanceof RootWrapper ||
           element instanceof NamedLockWrapper ||
           element instanceof AutolockWrapper ||
           element instanceof BootClassWrapper ||
           element instanceof TransientFieldWrapper ||
           element instanceof DistributedMethodWrapper ||
           element instanceof IncludeWrapper ||
           element instanceof ExcludeWrapper) {
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

    if(provider != null) {
      return provider.getSelection();
    }

    return null;
  }

  public void run() {
    fPart.removeSelectedItem();
  }
}
