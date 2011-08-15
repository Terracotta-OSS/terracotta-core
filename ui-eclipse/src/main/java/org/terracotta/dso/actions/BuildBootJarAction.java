/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.BootClassHelper;
import org.terracotta.dso.BootJarHelper;
import org.terracotta.dso.ClasspathProvider;
import org.terracotta.dso.ConfigSpec;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.QualifiedNames;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.dialogs.ExceptionDialog;

import com.tc.util.ProductInfo;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class BuildBootJarAction extends Action implements IActionDelegate, IWorkbenchWindowActionDelegate,
    IJavaLaunchConfigurationConstants, IProjectAction, IRunnableWithProgress {
  private IJavaProject        m_javaProject;
  private ConfigSpec          m_configSpec;
  private IAction             m_action;
  private String              m_jreContainerPath;
  private IProcess            m_process;

  private static final String LAUNCH_LABEL       = "DSO BootJar Creator";
  private static final String MAIN_TYPE          = "com.tc.object.tools.BootJarTool";
  private static final String CLASSPATH_PROVIDER = "org.terracotta.dso.classpathProvider";
  private static final String EXCEPTION_TITLE    = "Terracotta DSO";
  private static final String EXCEPTION_MESSAGE  = "Problem Building BootJar";

  public BuildBootJarAction() {
    super("Build BootJar...");
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public BuildBootJarAction(IJavaProject javaProject) {
    super("Build BootJar...");
    m_javaProject = javaProject;
  }

  public void setConfigSpec(ConfigSpec configSpec) {
    m_configSpec = configSpec;
  }

  public void setJREContainerPath(String path) {
    m_jreContainerPath = path;
  }

  public void run(IAction action) {
    try {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      window.run(true, true, this);
    } catch (Exception e) {
      Throwable cause = e.getCause();
      Shell activeShell = TcPlugin.getActiveWorkbenchShell();
      if (activeShell != null) {
        ExceptionDialog dialog = new ExceptionDialog(activeShell, EXCEPTION_TITLE, EXCEPTION_MESSAGE, cause);
        dialog.open();
      } else {
        TcPlugin.getDefault().openError("Building bootjar", cause);
      }
    }
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException {
    try {
      monitor.beginTask("Creating DSO BootJar...", IProgressMonitor.UNKNOWN);
      doFinish(monitor);
    } catch (Exception e) {
      throw new InvocationTargetException(e);
    } finally {
      monitor.done();
    }
  }

  private void doFinish(final IProgressMonitor monitor) throws Exception {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);
    ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);

    checkCancel(monitor);
    monitor.subTask("Please wait...");

    for (int i = 0; i < configs.length; i++) {
      ILaunchConfiguration config = configs[i];

      if (config.getName().equals(LAUNCH_LABEL)) {
        config.delete();
        break;
      }
    }

    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, LAUNCH_LABEL);
    IProject project = m_javaProject.getProject();

    String portablePath = m_jreContainerPath;
    if (portablePath == null) {
      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(m_javaProject);
      if (jreEntry != null) {
        IPath jrePath = jreEntry.getPath();
        if (jrePath != null) {
          portablePath = jrePath.makeAbsolute().toPortableString();
        }
      }
    }

    TcPlugin plugin = TcPlugin.getDefault();
    IFile configFile = plugin.getConfigurationFile(project);
    IPath configPath = configFile != null ? configFile.getLocation() : null;
    String bootJarName = BootJarHelper.getHelper().getBootJarName(portablePath);
    IPath outPath = project.getLocation().append(bootJarName);
    String args = "-v -w -o " + toOSString(outPath);

    String origVMArgs = wc.getAttribute(ATTR_VM_ARGUMENTS, "") + " ";
    String vmargs;
    IPath jarPath = TcPlugin.getDefault().getLibDirPath().append("tc.jar");
    if (jarPath.toFile().exists()) {
      String installPath = plugin.getLocation().makeAbsolute().toOSString();
      vmargs = "-Dtc.install-root=\"" + installPath + "\"";
    } else {
      vmargs = "-Dtc.classpath=\"" + ClasspathProvider.makeDevClasspath() + "\" -Dtc.install-root=\""
               + System.getProperty("tc.install-root") + "\"";
    }

    if (m_configSpec != null) vmargs += " -Dtc.config=\"" + m_configSpec.getSpec() + "\"";
    else if (configPath != null) args += " -f " + toOSString(configPath);

    wc.setAttribute(ATTR_VM_ARGUMENTS, vmargs + origVMArgs);
    wc.setAttribute(ATTR_CLASSPATH_PROVIDER, CLASSPATH_PROVIDER);
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, MAIN_TYPE);
    wc.setAttribute(ATTR_PROGRAM_ARGUMENTS, args);
    wc.setAttribute(ATTR_JRE_CONTAINER_PATH, portablePath);

    String runMode = ILaunchManager.RUN_MODE;
    JavaLaunchDelegate delegate = new JavaLaunchDelegate();
    Launch launch = new Launch(wc, runMode, null);

    checkCancel(monitor);
    delegate.launch(wc, runMode, launch, null);
    checkCancel(monitor);

    m_process = launch.getProcesses()[0];

    IStreamsProxy streamsProxy = m_process.getStreamsProxy();
    IStreamMonitor outMonitor = streamsProxy.getOutputStreamMonitor();
    IStreamMonitor errMonitor = streamsProxy.getErrorStreamMonitor();

    outMonitor.addListener(new IStreamListener() {
      public void streamAppended(final String text, IStreamMonitor streamMonitor) {
        System.err.print(text);
        monitor.subTask(text);
        monitor.worked(1);
      }
    });

    checkCancel(monitor);
    while (!m_process.isTerminated()) {
      checkCancel(monitor);
      ThreadUtil.reallySleep(100);
    }

    if (monitor.isCanceled()) {
      m_process = null;
      return;
    }

    if (m_process.getExitValue() != 0) {
      m_process = null;
      monitor.done();
      throw new RuntimeException(errMonitor.getContents());
    } else {
      project.refreshLocal(IResource.DEPTH_INFINITE, null);

      File outFile = outPath.toFile();
      if (outFile.exists()) { // it better exist, the process didn't return an error code
        BootClassHelper.cacheBootTypes(outFile);
        storeCreationProperty(project.getFile(bootJarName));
      }
      plugin.setBootClassHelper(project, new BootClassHelper(m_javaProject, bootJarName));
    }

    m_process = null;
  }

  private static void storeCreationProperty(IFile bootJarFile) {
    try {
      bootJarFile.setPersistentProperty(QualifiedNames.BOOT_JAR_PRODUCT_VERSION, ProductInfo.getInstance().version());
    } catch (CoreException ce) {
      /* ignore */
    }
  }

  private void checkCancel(IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      try {
        if (m_process != null && !m_process.isTerminated()) {
          m_process.terminate();
        }
      } catch (Exception e) {/**/
      }
    }
  }

  private static String toOSString(IPath path) {
    return "\"" + path.makeAbsolute().toOSString() + "\"";
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_action = action;

    if (m_javaProject == null || selection instanceof IStructuredSelection) {
      update(ActionUtil.locateSelectedJavaProject(selection));
    } else {
      action.setEnabled(true);
    }
  }

  private void update(IJavaProject javaProject) {
    if (javaProject != null) {
      try {
        if (javaProject.getProject().hasNature(ProjectNature.NATURE_ID)) {
          m_javaProject = javaProject;
        } else {
          m_javaProject = null;
        }
      } catch (CoreException ce) {/**/
      }
    } else {
      m_javaProject = null;
    }

    m_action.setEnabled(m_javaProject != null);
  }

  public void update(IProject project) {
    update(ActionUtil.findJavaProject(project));
  }

  public void dispose() {
    /**/
  }

  public void init(IWorkbenchWindow window) {
    /**/
  }
}
