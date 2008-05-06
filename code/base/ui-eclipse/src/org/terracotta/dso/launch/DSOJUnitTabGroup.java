/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;

public class DSOJUnitTabGroup extends AbstractLaunchConfigurationTabGroup {
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {    
    ILaunchConfigurationTab[] tabs= new ILaunchConfigurationTab[] {
      new JUnitLaunchConfigurationTab(),
      new JavaArgumentsTab(),
      new JavaClasspathTab(),
      new JavaJRETab(),
      new SourceLookupTab(),
      new EnvironmentTab(),
      new ConfigurationTab(),
      new CommonTab()
    };
    setTabs(tabs);
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config); 
    AssertionVMArg.setArgDefault(config);
  }
}
