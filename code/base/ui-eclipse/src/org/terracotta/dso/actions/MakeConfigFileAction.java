/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.terracotta.dso.TcPlugin;

public class MakeConfigFileAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate
{
  private IFile m_file;
  
  public MakeConfigFileAction() {
    super("Make Current Config File");
  }

  public void run(IAction action) {
    TcPlugin.getDefault().setup(m_file);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      m_file = (IFile)((IStructuredSelection)selection).getFirstElement();
    }
  }

  public void dispose() {/**/}
  public void init(IWorkbenchWindow window) {/**/}
}
