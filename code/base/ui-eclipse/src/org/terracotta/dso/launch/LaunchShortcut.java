/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.core.ILaunchConfigurationType;

public class LaunchShortcut extends org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut {
  /**
   * Renamed to getConfigurationType in 3.2
   */
  protected ILaunchConfigurationType getJavaLaunchConfigType() {
    return internalGetJavaLaunchConfigType();    
  }

  /**
   * Renamed from getJavaLaunchConfigType in 3.1
   */
  protected ILaunchConfigurationType getConfigurationType() {
    return internalGetJavaLaunchConfigType();    
  }

  /**
   * Bridge to span renaming of getJavaLaunchConfigType -> getConfigurationType in 3.2
   */
  private ILaunchConfigurationType internalGetJavaLaunchConfigType() {
    return getLaunchManager().getLaunchConfigurationType("launch.configurationDelegate");    
  }
}
