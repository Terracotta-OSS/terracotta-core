package org.terracotta.dso.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.terracotta.dso.TcPlugin;

/**
 * Supports Eclipse 3.3 and earlier.
 * 
 * @see LaunchShortcut
 * @see LaunchShortcut34
 * @see org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut
 */
public class LaunchShortcut33 extends org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut implements
    IDSOLaunchConfigurationConstants {
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
    return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType("launch.configurationDelegate");
  }

  protected ILaunchConfiguration createConfiguration(IType type) {
    ILaunchConfiguration config = super.createConfiguration(type);
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
      config = wc.doSave();
    } catch (CoreException exception) {
      String title = "Problem creating launch configuration";
      MessageDialog.openError(TcPlugin.getActiveWorkbenchShell(), title, exception.getStatus().getMessage()); 
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
