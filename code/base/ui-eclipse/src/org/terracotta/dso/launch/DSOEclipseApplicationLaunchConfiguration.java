/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.ui.launcher.EclipseApplicationLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.terracotta.dso.TcPlugin;

import java.util.ArrayList;

/**
 * Launcher for DSO Eclipse applications.
 */

public class DSOEclipseApplicationLaunchConfiguration extends EclipseApplicationLaunchConfiguration implements
    IDSOLaunchDelegate {

  private final LaunchHelper fLaunchHelper = new LaunchHelper(this);

  @Override
  public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch,
                     final IProgressMonitor monitor) throws CoreException {
    final ILaunchConfigurationWorkingCopy wc = this.fLaunchHelper.setup(config, mode, launch, monitor);
    if (wc != null) {
      super.launch(wc, mode, launch, monitor);
    }
  }

  public IJavaProject getJavaProject(final ILaunchConfiguration configuration) throws CoreException {
    String projectName = getJavaProjectName(configuration);

    if (projectName != null) {
      projectName = projectName.trim();

      if (projectName.length() > 0) {
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        final IJavaProject javaProject = JavaCore.create(project);

        if (javaProject != null && javaProject.exists()) { return javaProject; }
      }
    }

    return null;
  }

  public String getJavaProjectName(final ILaunchConfiguration configuration) throws CoreException {
    @SuppressWarnings("static-access")
    String appNameRoot = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);

    if (appNameRoot != null) {
      appNameRoot = appNameRoot.substring(0, appNameRoot.lastIndexOf('.'));
    } else {
      final String msg = "No application specified for launch configuration '" + configuration.getName() + "'";
      final Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
      throw new CoreException(status);
    }
    final IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (final IProject proj : projs) {
      final IPluginModelBase base = PluginRegistry.findModel(proj);
      if (base != null && appNameRoot.equals(base.getPluginBase().getId())) { return proj.getName(); }
    }
    final String msg = "Unable to determine project for pluginId '" + appNameRoot + "'";
    final Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
    throw new CoreException(status);
  }

  public IVMInstall getVMInstall(final ILaunchConfiguration configuration) throws CoreException {
    @SuppressWarnings("static-access")
    final String vm = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);

    final IVMInstall launcher = getVMInstall(vm);
    if (launcher == null) {
      final String msg = "Cannot locate VMInstall for '" + vm + "'";
      final Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, null);
      throw new CoreException(status);
    }
    return launcher;
  }

  public static IVMInstall getVMInstall(final String name) {
    if (name != null) {
      final IVMInstall[] installs = getAllVMInstances();
      for (final IVMInstall install : installs) {
        if (install.getName().equals(name)) { return install; }
      }
    }
    return JavaRuntime.getDefaultVMInstall();
  }

  public static IVMInstall[] getAllVMInstances() {
    final ArrayList res = new ArrayList();
    final IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
    for (final IVMInstallType type : types) {
      final IVMInstall[] installs = type.getVMInstalls();
      for (final IVMInstall install : installs) {
        res.add(install);
      }
    }
    return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
  }
}
