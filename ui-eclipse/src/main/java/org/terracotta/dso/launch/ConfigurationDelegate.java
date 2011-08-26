/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

/**
 * Launcher for standard DSO applications. 
 */

public class ConfigurationDelegate extends JavaLaunchDelegate implements IDSOLaunchDelegate {
  private LaunchHelper fLaunchHelper = new LaunchHelper(this);

  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    ILaunchConfigurationWorkingCopy wc = fLaunchHelper.setup(config, mode, launch, monitor);
    if(wc != null) {
      super.launch(wc, mode, launch, monitor);
    }
  }
}
