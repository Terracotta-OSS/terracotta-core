/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;

/**
 * We will probably need some launch properties sooner or later.
 */

public class ConfigurationTab extends AbstractLaunchConfigurationTab {
  public void createControl(Composite parent) {/**/}
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {/**/}
  public void initializeFrom(ILaunchConfiguration configuration) {/**/}
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {/**/}

  public String getName() {
    return "Terracotta DSO";
  }
}
