/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.BootJarHelper;
import org.terracotta.dso.ExceptionDialog;
import org.terracotta.dso.ProjectNature;
import org.terracotta.dso.TcPlugin;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class BuildBootJarAction extends Action
  implements IActionDelegate,
             IWorkbenchWindowActionDelegate,
             IJavaLaunchConfigurationConstants,
             IProjectAction
{
  private IJavaProject m_javaProject;
  private IAction      m_action;
  private String       m_jreContainerPath;
  
  private static final String LAUNCH_LABEL       = "DSO BootJar Creator";
  private static final String MAIN_TYPE          = "com.tc.object.tools.BootJarTool";
  private static final String CLASSPATH_PROVIDER = "org.terracotta.dso.classpathProvider";
  
  public BuildBootJarAction() {
    super("Build BootJar...");
    TcPlugin.getDefault().registerProjectAction(this);
  }

  public BuildBootJarAction(IJavaProject javaProject) {
    super("Build BootJar...");
    m_javaProject = javaProject;
  }
  
  public void setJREContainerPath(String path) {
    m_jreContainerPath = path;
  }
  
  public void run(IAction action) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    
    if(!workbench.saveAllEditors(true)) {
      return;
    }
    
    try {
      IRunnableWithProgress op = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor)
          throws InvocationTargetException
        {
          try {
            monitor.beginTask("Creating DSO BootJar...", IProgressMonitor.UNKNOWN);
            doFinish(monitor);
          } catch(Exception e) {
            throw new InvocationTargetException(e);
          } finally {
            monitor.done();
          }
        }
      };

      new ProgressMonitorDialog(null).run(true, false, op);
    }
    catch(InterruptedException e) {
      /**/
    }
    catch(final InvocationTargetException ite) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          Throwable       t = ite.getCause();
          ExceptionDialog d = new ExceptionDialog();
          
          d.setTitle("Terracotta DSO");
          d.setMessage("Problem building BootJar");
          d.setErrorText(t.getMessage());
          d.center();
          d.setVisible(true);
        }
      });
    }
    catch(final Exception e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ExceptionDialog d = new ExceptionDialog();
          
          d.setTitle("Terracotta DSO");
          d.setMessage("Problem building BootJar");
          d.setError(e);
          d.center();
          d.setVisible(true);
        }
      });
    }
  }

  private void doFinish(final IProgressMonitor monitor) throws Exception {
    ILaunchManager           manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type    = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);
    ILaunchConfiguration[]   configs = manager.getLaunchConfigurations(type);
    
    for(int i = 0; i < configs.length; i++) {
      ILaunchConfiguration config = configs[i];
      
      if(config.getName().equals(LAUNCH_LABEL)) {
        config.delete();
        break;
      }
    }
    
    ILaunchConfigurationWorkingCopy wc      = type.newInstance(null, LAUNCH_LABEL);
    IProject                        project = m_javaProject.getProject();
    
    String portablePath = m_jreContainerPath;
    if(portablePath == null) {
      IPath jrePath = JavaRuntime.computeJREEntry(m_javaProject).getPath();
      if(jrePath != null) {
        portablePath = jrePath.toPortableString();
      }
    }

    TcPlugin plugin      = TcPlugin.getDefault();
    IFile    configFile  = plugin.getConfigurationFile(project);
    IPath    configPath  = configFile.getLocation();
    String   bootJarName = BootJarHelper.getHelper().getBootJarName(portablePath);
    IPath    outPath     = project.getLocation().append(bootJarName);
    String   args        = "-o " + toOSString(outPath) + " -f " + toOSString(configPath);
    
    if(configFile == null) {
      throw new RuntimeException("No config file");
    }
      
    if(!configFile.exists()) {
      throw new FileNotFoundException(toOSString(configPath));
    }
    
    wc.setAttribute(ATTR_MAIN_TYPE_NAME, MAIN_TYPE);
    wc.setAttribute(ATTR_PROGRAM_ARGUMENTS, args);
    wc.setAttribute(ATTR_CLASSPATH_PROVIDER, CLASSPATH_PROVIDER);
    wc.setAttribute(ATTR_JRE_CONTAINER_PATH, portablePath);
    
    String             runMode  = ILaunchManager.RUN_MODE;
    JavaLaunchDelegate delegate = new JavaLaunchDelegate();
    Launch             launch   = new Launch(wc, runMode, null);

    delegate.launch(wc, runMode, launch, null);
    
    IProcess       process      = launch.getProcesses()[0];
    IStreamsProxy  streamsProxy = process.getStreamsProxy();
    IStreamMonitor outMonitor   = streamsProxy.getOutputStreamMonitor();    
    IStreamMonitor errMonitor   = streamsProxy.getErrorStreamMonitor();
    
    outMonitor.addListener(new IStreamListener() {
      public void streamAppended(final String text, IStreamMonitor streamMonitor) {
        monitor.subTask(text);
        monitor.worked(1);
      }
    });
    
    while(!process.isTerminated()) {
      try {
        Thread.sleep(100);
      } catch(Exception e) {/**/}
    }
    
    if(process.getExitValue() != 0) {
      throw new RuntimeException(errMonitor.getContents());
    }
    else {
      project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }
  }
  
  private static String toOSString(IPath path) {
    return "\"" + path.makeAbsolute().toOSString() + "\"";
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    m_action = action;
    
    if(m_javaProject == null || selection instanceof IStructuredSelection) {
      update(ActionUtil.locateSelectedJavaProject(selection));
    }
    else {
      action.setEnabled(true);
    }
  }

  private void update(IJavaProject javaProject) {
    if(javaProject != null) {
      try {
        if(javaProject.getProject().hasNature(ProjectNature.NATURE_ID)) {
          m_javaProject = javaProject;
        }
        else {
          m_javaProject = null;
        }
      } catch(CoreException ce) {/**/}
    }
    else {
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
