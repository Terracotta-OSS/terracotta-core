/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.wizards;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;

import org.terracotta.dso.TcPlugin;

public class NewProjectWizard extends JavaProjectWizard {
  public NewProjectWizard() {
    super();
  }

  public boolean performFinish() {
    boolean result = super.performFinish();
    
    if(result) {
      IJavaProject  javaProject = (IJavaProject)getCreatedElement();
      ProjectWizard wizard      = new ProjectWizard(javaProject);
      Shell         shell       = getShell();
      
      try {
        new ProgressMonitorDialog(shell).run(false, true, wizard.getWorker());
      }
      catch(Exception e) {
        TcPlugin plugin = TcPlugin.getDefault();
        
        plugin.openError("Problem creating Terracotta project", e);
        plugin.removeTerracottaNature(javaProject);
      }
    }
  
    return result;
  }
}
