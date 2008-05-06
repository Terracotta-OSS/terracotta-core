/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.ui.launcher.AbstractPDELaunchConfigurationTabGroup;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.pde.ui.launcher.MainTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class DSOEclipseApplicationLaunchConfigurationTabGroup extends AbstractPDELaunchConfigurationTabGroup {
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = null;
    tabs = new ILaunchConfigurationTab[]{
        new MainTab(),
        new JavaArgumentsTab(),
        new PluginsTab(), 
        new ConfigurationTab(),
        new TracingTab(), 
        new EnvironmentTab(),
        new org.terracotta.dso.launch.ConfigurationTab(),
        new CommonTab()};
    setTabs(tabs);
  }
}
