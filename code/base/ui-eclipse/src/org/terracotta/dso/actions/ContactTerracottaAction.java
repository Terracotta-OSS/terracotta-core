/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.tc.admin.common.BrowserLauncher;

public class ContactTerracottaAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate
{
  public ContactTerracottaAction() {
    super();
  }

  public void run(IAction action) {
    String id  = action.getId();
    String url = null;
    
    if(id.equals("visitForumsAction")) {
      url = "http://www.terracottatech.com/forums/";
    }
    else if(id.equals("contactSupportAction")) {
      url = "http://www.terracottatech.com/support_services.shtml";
    }
    else if(id.equals("contactFieldAction")) {
      url = "http://www.terracottatech.com/contact/field/";
    }
    else if(id.equals("contactSalesAction")) {
      url = "http://www.terracottatech.com/contact/";
    }

    if(url != null) {
      BrowserLauncher.openURL(url);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {/**/}
  public void dispose() {/**/}
  public void init(IWorkbenchWindow window) {/**/}
}
