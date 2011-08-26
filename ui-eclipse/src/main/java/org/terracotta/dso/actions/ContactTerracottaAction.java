/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.tc.admin.common.BrowserLauncher;
import com.tc.util.ProductInfo;

import java.text.MessageFormat;

public class ContactTerracottaAction extends Action implements IActionDelegate, IWorkbenchWindowActionDelegate {
  public ContactTerracottaAction() {
    super();
  }

  private static String getKitID() {
    String kitID = ProductInfo.getInstance().kitID();
    if (kitID == null || ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
      if ((kitID = System.getProperty("com.tc.kitID")) == null) {
        kitID = "3.0";
      }
    }
    return kitID;
  }

  public void run(IAction action) {
    String id = action.getId();
    String url = null;
    String kitID = getKitID();

    if (id.equals("visitForumsAction")) {
      url = MessageFormat.format("http://www.terracotta.org/kit/reflector?kitID={0}&pageID=Forums", kitID);
    } else if (id.equals("contactSupportAction")) {
      url = MessageFormat.format("http://www.terracotta.org/kit/reflector?kitID={0}&pageID=SupportServices", kitID);
    }

    if (url != null) {
      BrowserLauncher.openURL(url);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {/**/
  }

  public void dispose() {/**/
  }

  public void init(IWorkbenchWindow window) {/**/
  }
}
