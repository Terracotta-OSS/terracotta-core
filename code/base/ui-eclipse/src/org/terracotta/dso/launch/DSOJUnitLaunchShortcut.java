/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.terracotta.dso.TcPlugin;

public class DSOJUnitLaunchShortcut extends JUnitLaunchShortcut implements IDSOLaunchConfigurationConstants {
  protected String getLaunchConfigurationTypeId() {
    return "launch.junitTestConfigurationDelegate";
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
    ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(element);
    ILaunchConfigurationWorkingCopy wc = null;
    try {
      wc = config.getWorkingCopy();
      IFile configFile = TcPlugin.getDefault().getConfigurationFile(getProject(wc));

      if (configFile != null) {
        String arg = configFile.getFullPath().toString();
        IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
        String configSpec = variableManager.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
        wc.setAttribute(ID_CONFIG_FILE_SPEC, configSpec);
      }
      config = wc;
    } catch (CoreException exception) {
      TcPlugin.getDefault().openError("JavaElement("+element+")", exception);
    }
    return config;
  }

  private IProject getProject(ILaunchConfiguration configuration) throws CoreException {
    String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
    if (projectName != null) {
      projectName = projectName.trim();
      if (projectName.length() > 0) { return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName); }
    }
    return null;
  }
}
