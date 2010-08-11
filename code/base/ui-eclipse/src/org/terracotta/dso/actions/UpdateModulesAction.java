/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.terracotta.dso.ClasspathProvider;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.dialogs.ExceptionDialog;

import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class UpdateModulesAction extends Action implements IActionDelegate, IWorkbenchWindowActionDelegate,
    IJavaLaunchConfigurationConstants, IRunnableWithProgress {
  private IProcess            m_process;

  private static final String LAUNCH_LABEL       = "TIM-Get";
  private static final String MAIN_TYPE          = "org.terracotta.tools.cli.TIMGetTool";
  private static final String CLASSPATH_PROVIDER = "org.terracotta.dso.classpathProvider";
  private static final String EXCEPTION_TITLE    = "Terracotta DSO";
  private static final String EXCEPTION_MESSAGE  = "Problem Updating Integration Modules";

  public UpdateModulesAction() {
    super("Update Integration Modules...");
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
        TcPlugin.getDefault().openError("Updating Integration Modules", cause);
      }
    }
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException {
    try {
      monitor.beginTask("Updating Integration Modules...", IProgressMonitor.UNKNOWN);
      doFinish(monitor);
      monitor.done();
    } catch (Exception e) {
      throw new InvocationTargetException(e);
    }
  }

  private void doFinish(final IProgressMonitor monitor) throws Exception {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);
    ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);

    checkCancel(monitor);
    monitor.subTask("Please wait...");

    for (ILaunchConfiguration config : configs) {
      if (config.getName().equals(LAUNCH_LABEL)) {
        config.delete();
        break;
      }
    }

    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, LAUNCH_LABEL);
    String args = "install --all";
    TcPlugin plugin = TcPlugin.getDefault();
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

    wc.setAttribute(ATTR_VM_ARGUMENTS, vmargs + origVMArgs);
    wc.setAttribute(ATTR_CLASSPATH_PROVIDER, CLASSPATH_PROVIDER);
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, MAIN_TYPE);
    wc.setAttribute(ATTR_PROGRAM_ARGUMENTS, args);

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
    IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
    IOConsole console = new IOConsole("Terracotta", null);
    consoleManager.addConsoles(new IConsole[] { console });
    final IOConsoleOutputStream outStream = console.newOutputStream();

    outMonitor.addListener(new IStreamListener() {
      public void streamAppended(final String text, IStreamMonitor streamMonitor) {
        try {
          outStream.write(text);
        } catch (IOException ioe) {
          /**/
        }
      }
    });

    checkCancel(monitor);
    consoleManager.showConsoleView(console);

    while (!m_process.isTerminated()) {
      checkCancel(monitor);
      ThreadUtil.reallySleep(100);
    }

    outStream.close();

    if (!monitor.isCanceled() && m_process.getExitValue() != 0) {
      m_process = null;
      monitor.done();
      throw new RuntimeException(errMonitor.getContents());
    }

    m_process = null;
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

  public void selectionChanged(IAction action, ISelection selection) {
    /**/
  }

  public void dispose() {
    /**/
  }

  public void init(IWorkbenchWindow window) {
    /**/
  }
}
