/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.actions.ActionContext;
import org.terracotta.ui.util.SelectionUtil;

public class HonorTransientAction extends Action {
  ConfigViewPart fPart;
  
  HonorTransientAction(ConfigViewPart part) {
    super("Honor transient", AS_CHECK_BOX);
    fPart = part;
  }
  
  public void run() {
    fPart.setHonorTransient(isChecked());
  }

  public void setContext(ActionContext context) {
    Object element = SelectionUtil.getSingleElement(getSelection());

    if(element instanceof IncludeWrapper) {
      IncludeWrapper wrapper = (IncludeWrapper)element;
      setChecked(wrapper.getHonorTransient());
    }
  }

  public boolean canActionBeAdded() {
    Object element = SelectionUtil.getSingleElement(getSelection());
    return element instanceof IncludeWrapper;
  }
  
  private ISelection getSelection() {
    ISelectionProvider provider = fPart.getSite().getSelectionProvider();
    return (provider != null) ? provider.getSelection() : null;
  }
}
