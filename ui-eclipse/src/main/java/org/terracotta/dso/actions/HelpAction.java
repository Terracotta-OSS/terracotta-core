/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.tc.admin.common.BrowserLauncher;

public class HelpAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate
{
  private String m_url;
  
  public HelpAction(String label, String url) {
    super(label);
    m_url = url;
  }

  public void run(IAction action) {
    run();
  }

  public void run() {
    if(m_url != null) {
      BrowserLauncher.openURL(m_url);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {/**/}
  public void dispose() {/**/}
  public void init(IWorkbenchWindow window) {/**/}
}
