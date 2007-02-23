/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchShortcut;

public class DSOJUnitLaunchShortcut extends JUnitLaunchShortcut {
  protected ILaunchConfigurationType getJUnitLaunchConfigType() {
    return getLaunchManager().getLaunchConfigurationType("launch.junitTestConfigurationDelegate");    
  }
}
