/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.ui.launcher.EclipseApplicationLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.BootJarHelper;
import org.terracotta.dso.ClasspathProvider;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.BuildBootJarAction;

public class DSOEclipseApplicationLaunchConfiguration extends EclipseApplicationLaunchConfiguration implements
    IJavaLaunchConfigurationConstants {
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    try {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          IWorkbench workbench = PlatformUI.getWorkbench();
          if(workbench != null) {
            workbench.saveAllEditors(false);
          }
        }
      });

      ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
      final IJavaProject javaProject = getJavaProject(wc);
      final IProject project = javaProject.getProject();

      final TcPlugin plugin = TcPlugin.getDefault();
      String vmArgs = wc.getAttribute(ATTR_VM_ARGUMENTS, "");
      IPath libDirPath = plugin.getLibDirPath();
      IFile configFile = plugin.getConfigurationFile(project);

      if (!plugin.continueWithConfigProblems(project)) { return; }

      final ServerTracker tracker = ServerTracker.getDefault();
      if (!tracker.anyRunning(javaProject)) {
        tracker.startServer(javaProject, plugin.getAnyServerName(project));
      }

      IPath configPath = configFile.getLocation();
      String configProp = " -Dtc.config=\"" + toOSString(configPath) + "\"";
      
      String portablePath = null;
      IPath jrePath = JavaRuntime.computeJREEntry(javaProject).getPath();
      if(jrePath != null) {
        portablePath = jrePath.toPortableString();
      }

      String jreContainerPath = wc.getAttribute(ATTR_JRE_CONTAINER_PATH, portablePath);
      String bootJarName = BootJarHelper.getHelper().getBootJarName(jreContainerPath);

      if (bootJarName == null || bootJarName.length() == 0) {
        IVMInstall vmInstall = getVMInstall(wc);
        String vmName;

        if (vmInstall != null) {
          vmName = vmInstall.getName();
        } else {
          vmName = jreContainerPath.substring(jreContainerPath.lastIndexOf('/') + 1);
        }

        throw new RuntimeException("Can't determine BootJar name for runtime '" + vmName + "'");
      }

      IFile localBootJar = project.getFile(bootJarName);
      IPath bootPath;

      testEnsureBootJar(plugin, javaProject, localBootJar, jreContainerPath);

      if (localBootJar.exists()) {
        bootPath = localBootJar.getLocation();
      } else {
        bootPath = BootJarHelper.getHelper().getBootJarPath(bootJarName);
      }

      String bootProp = " -Xbootclasspath/p:\"" + toOSString(bootPath) + "\"";

      if (!configPath.toFile().exists()) {
        String path = configPath.toOSString();
        plugin.openError("Project config file '" + path + "' not found", new RuntimeException("tc.config not found: "
                                                                                              + path));
      }

      if (!bootPath.toFile().exists()) {
        String path = bootPath.toOSString();
        plugin.openError("System bootjar '" + path + "' not found", new RuntimeException("bootjar not found: " + path));
      }

      String cpProp;
      if (libDirPath.append("tc.jar").toFile().exists()) {
        cpProp = " -Dtc.install-root=\"" + toOSString(plugin.getLocation()) + "\"";
      } else {
        cpProp = " -Dtc.classpath=\"" + ClasspathProvider.makeDevClasspath() + "\"";
      }

      wc.setAttribute(ATTR_VM_ARGUMENTS, cpProp + configProp + bootProp + " " + vmArgs);

      super.launch(wc, mode, launch, monitor);
    } catch (Throwable t) {
      String msg = "Unable to launch '" + config.getName() + "'\n\n" + t.getLocalizedMessage();
      Status status = new Status(IStatus.ERROR, TcPlugin.getPluginId(), 1, msg, t);
      throw new CoreException(status);
    }
  }

  private static String toOSString(IPath path) {
    return path.makeAbsolute().toOSString();
  }

  private void testEnsureBootJar(final TcPlugin     plugin,
                                 final IJavaProject javaProject,
                                 final IFile        bootJar,
                                 final String       jreContainerPath)
  {
    IProject            project                 = javaProject.getProject();
    ConfigurationHelper configHelper            = plugin.getConfigurationHelper(project);
    IFile               configFile              = plugin.getConfigurationFile(project);
    boolean             stdBootJarExists        = false;
    boolean             configHasBootJarClasses = configHelper.hasBootJarClasses();
    
    try {
      stdBootJarExists = BootJarHelper.getHelper().getBootJarFile().exists();
    } catch (CoreException ce) {/**/
    }

    if (!stdBootJarExists || (configFile != null && configHasBootJarClasses)) {
      long bootStamp = bootJar.getLocalTimeStamp();
      long confStamp = configFile.getLocalTimeStamp();

      if (!bootJar.exists() || (configHasBootJarClasses && bootStamp < confStamp)) {
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            BuildBootJarAction bbja = new BuildBootJarAction(javaProject);
            bbja.setJREContainerPath(jreContainerPath);
            bbja.run(null);
          }
        });
      }
    }
  }

  public IJavaProject getJavaProject(ILaunchConfiguration configuration) throws CoreException {
    String projectName = getJavaProjectName(configuration);
    
    if (projectName != null) {
      projectName = projectName.trim();
      
      if (projectName.length() > 0) {
        IProject     project     = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        IJavaProject javaProject = JavaCore.create(project);
        
        if (javaProject != null && javaProject.exists()) {
          return javaProject;
        }
      }
    }
    
    return null;
  }

  public String getJavaProjectName(ILaunchConfiguration configuration) throws CoreException {
    String appNameRoot = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
    if(appNameRoot != null) {
      appNameRoot = appNameRoot.substring(0, appNameRoot.lastIndexOf('.'));
    }
    PluginModelManager manager = PDECore.getDefault().getModelManager();
    IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (int i = 0; i < projs.length; i++) {
      if (!WorkspaceModelManager.isPluginProject(projs[i])) {
        continue;
      }
      IPluginModelBase base = manager.findModel(projs[i]);
      if(appNameRoot.equals(base.getPluginBase().getId())) {
        return projs[i].getName();
      }
    }
    return null;
  }

  public IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
    return JavaRuntime.computeVMInstall(configuration);
  }
}
