/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.ui.launcher.EclipseApplicationLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.terracotta.dso.TcPlugin;

import java.util.ArrayList;

/**
 * Launcher for DSO Eclipse applications. 
 */

public class DSOEclipseApplicationLaunchConfiguration extends EclipseApplicationLaunchConfiguration implements
    IDSOLaunchDelegate {

  private LaunchHelper fLaunchHelper = new LaunchHelper(this);

  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    ILaunchConfigurationWorkingCopy wc = fLaunchHelper.setup(config, mode, launch, monitor);
    if(wc != null) {
      super.launch(wc, mode, launch, monitor);
    }
  }

  public IJavaProject getJavaProject(ILaunchConfiguration configuration) throws CoreException {
    String projectName = getJavaProjectName(configuration);

    if (projectName != null) {
      projectName = projectName.trim();

      if (projectName.length() > 0) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        IJavaProject javaProject = JavaCore.create(project);

        if (javaProject != null && javaProject.exists()) { return javaProject; }
      }
    }

    return null;
  }

  public String getJavaProjectName(ILaunchConfiguration configuration) throws CoreException {
    String appNameRoot = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
    if (appNameRoot != null) {
      appNameRoot = appNameRoot.substring(0, appNameRoot.lastIndexOf('.'));
    } else {
      String msg = "No application specified for launch configuration '" + configuration.getName() + "'";
      Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
      throw new CoreException(status);
    }
    PluginModelManager manager = PDECore.getDefault().getModelManager();
    IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (int i = 0; i < projs.length; i++) {
      if (!WorkspaceModelManager.isPluginProject(projs[i])) {
        continue;
      }
      IPluginModelBase base = manager.findModel(projs[i]);
      if (appNameRoot.equals(base.getPluginBase().getId())) { return projs[i].getName(); }
    }
    String msg = "Unable to determine project for pluginId '" + appNameRoot + "'";
    Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
    throw new CoreException(status);
  }

  public IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
    String vm = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
    IVMInstall launcher = getVMInstall(vm);
    if (launcher == null) {
      String msg = "Cannot locate VMInstall for '"+vm+"'";
      Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
      throw new CoreException(status);
    }
    return launcher;
  }
  
  public static IVMInstall getVMInstall(String name) {
    if (name != null) {
      IVMInstall[] installs = getAllVMInstances();
      for (int i = 0; i < installs.length; i++) {
        if (installs[i].getName().equals(name))
          return installs[i];
      }
    }
    return JavaRuntime.getDefaultVMInstall();
  }

  public static IVMInstall[] getAllVMInstances() {
    ArrayList res = new ArrayList();
    IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
    for (int i = 0; i < types.length; i++) {
      IVMInstall[] installs = types[i].getVMInstalls();
      for (int k = 0; k < installs.length; k++) {
        res.add(installs[k]);
      }
    }
    return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
  }
}
