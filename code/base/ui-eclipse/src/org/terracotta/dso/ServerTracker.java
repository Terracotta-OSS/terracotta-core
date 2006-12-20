/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.tc.admin.TCStop;
import org.terracotta.dso.decorator.ServerRunningDecorator;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Used to start a server using the project's config information.
 * Listens for user-initiated termination requests and shuts down the
 * associated server.
 * 
 * A JavaProject can only have a single server running at one time.  A mapping
 * is maintained (m_server) between the IProcess and a ServerInfo.  ServerInfo
 * contains the IJavaProject and the JMX port of the running server. The JMX port
 * is used for stopping the server and it's done this way to because otherwise
 * we must rely on the configuration, which the user can change.
 * 
 * @see TcPlugin.launchServer
 */

public class ServerTracker {
  private static ServerTracker m_instance = new ServerTracker();
  private HashMap              m_servers  = new HashMap();
 
  /**
   * This name is used in plugin.xml for managing the start/stop menu items.
   */
  private static final QualifiedName SERVER_RUNNING_NAME =
    new QualifiedName("org.terracotta.dso", "ServerRunning");

  private ServerTracker() {
    super();
  }

  public static synchronized ServerTracker getDefault() {
    if(m_instance == null) {
      m_instance = new ServerTracker();
    }

    return m_instance;
  }
	
  private IDebugEventSetListener listener = new IDebugEventSetListener() {
    public void handleDebugEvents(DebugEvent[] events) {
      if(events != null && events.length > 0) {
        if(events[0].getKind() == DebugEvent.TERMINATE) {
          Object source = events[0].getSource();
  
          if(source instanceof IProcess) {
            ServerInfo serverInfo = (ServerInfo)m_servers.get(source);
  
            if(serverInfo != null) {
              m_servers.remove(source);
              if(m_servers.isEmpty()) {
                setRunning(serverInfo.getJavaProject(), null);
              }
            }
          }
        }
      }
    }
  };

  public boolean anyRunning(IJavaProject javaProj) {
    try {
      return javaProj.getProject().getSessionProperty(SERVER_RUNNING_NAME) != null;
    } catch(CoreException ce) {
      return false;
    }
  }
	
  public boolean isRunning(IJavaProject javaProj, String name) {
    if(anyRunning(javaProj)) {
      Iterator   iter = m_servers.keySet().iterator();
      IProcess   proc;
      ServerInfo serverInfo;
      String     serverName;
      
      while(iter.hasNext()) {
        proc       = (IProcess)iter.next();
        serverInfo = (ServerInfo)m_servers.get(proc);
        serverName = serverInfo.getName();

        if(name.equals(serverName)) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  public void setRunning(IJavaProject javaProj, Boolean value) {
    if(value != null && value.equals(Boolean.FALSE)) {
      value = null;
    }

    IProject project = javaProj.getProject();
    
    if(project.isOpen()) {
      try {
        project.setSessionProperty(SERVER_RUNNING_NAME, value);
        ServerRunningDecorator.updateDecorators();
      } catch(CoreException ce) {/**/}
    }
  }

  public void startServer(IJavaProject javaProject, String name)
    throws CoreException
  {
    if(isRunning(javaProject, name)) {
      internalStopServer(javaProject, true, name);
    }
    else {
      internalStartServer(javaProject, name);
    }
  }

  private void internalStartServer(IJavaProject javaProject, String name)
    throws CoreException
  {
    TcPlugin   plugin   = TcPlugin.getDefault();
    String     projName = javaProject.getElementName();
    int        jmxPort  = plugin.getJmxPort(javaProject.getProject(), name);
    ILaunch    launch   = plugin.launchServer(javaProject, projName, name);
    ServerInfo info     = new ServerInfo(javaProject, name, jmxPort != 0 ? jmxPort : 9520);
    
    m_servers.put(launch.getProcesses()[0], info);
    
    DebugPlugin.getDefault().addDebugEventListener(listener);
    setRunning(javaProject, Boolean.TRUE);
  }

  public void cancelServer(IJavaProject javaProject) {
    Iterator   iter = m_servers.keySet().iterator();
    IProcess   proc;
    ServerInfo info;

    while(iter.hasNext()) {
      proc = (IProcess)iter.next();
      info = (ServerInfo)m_servers.get(proc);
      
      if(info.getJavaProject().equals(javaProject)) {
        try {
          proc.terminate();
        } catch(DebugException de) {
          iter.remove();
        }
      }
    }
  }
  
  public void stopServer(IJavaProject javaProject) {
    internalStopServer(javaProject, false, null);
  }
 
  public void stopServer(IJavaProject javaProject, String name) {
    internalStopServer(javaProject, false, name);
  }

  private void internalStopServer(IJavaProject javaProject, boolean restart, String name) {
    IWorkbench       workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window    = workbench.getActiveWorkbenchWindow();
    Shell            shell     = window != null ? window.getShell() : null;
  
    try {
      IRunnableWithProgress op = new TCStopper(javaProject, restart, name);
      new ProgressMonitorDialog(shell).run(true, true, op);
    } catch(InvocationTargetException e) {
      TcPlugin.getDefault().openError("Cannot stop Terracotta server", e.getCause());
    } catch(InterruptedException e) {/**/}  
  }
  
  class TCStopper implements IRunnableWithProgress {
    IJavaProject m_javaProject;
    boolean      m_restart;
    String       m_name;
    
    TCStopper(IJavaProject javaProject) {
      this(javaProject, false, null);
    }
    
    TCStopper(IJavaProject javaProject, boolean restart, String name) {
      m_javaProject = javaProject;
      m_restart     = restart;
      m_name        = name;
    }
    
    public void run(IProgressMonitor monitor)
      throws InvocationTargetException
    {
      try {
        monitor.beginTask("Stopping Terracotta Server...", IProgressMonitor.UNKNOWN);
        doStopServer(m_javaProject, m_name, monitor);
        if(monitor.isCanceled()) {
          return;
        }
        if(m_restart) {
          internalStartServer(m_javaProject, m_name);
        }
      } catch(Exception e) {
        throw new InvocationTargetException(e);
      }
    }
  }
      
  private void doStopServer(IJavaProject targetProj, String targetName, IProgressMonitor monitor) {
    Iterator     iter = m_servers.keySet().iterator();
    IProcess     proc;
    ServerInfo   serverInfo;
    IJavaProject javaProject;
    int          jmxPort;
    String       name;
    
    while(iter.hasNext()) {
      proc        = (IProcess)iter.next();
      serverInfo  = (ServerInfo)m_servers.get(proc);
      javaProject = serverInfo.getJavaProject();
      jmxPort     = serverInfo.getJmxPort();
      name        = serverInfo.getName();
      
      if(javaProject.getProject().isOpen() &&
         targetProj.equals(javaProject) &&
         targetName.equals(name))
      {
        TCStop stopper = new TCStop("localhost", jmxPort != -1 ? jmxPort : 9520);
        
        try {
          stopper.stop();
          while(true) {
            if(monitor.isCanceled()) {
              return;
            }
            try {
              proc.getExitValue();
              return;
            } catch(DebugException de) {
              try {
                Thread.sleep(1000);
              } catch(InterruptedException ie) {/**/}
            }
          }
        } catch(Exception e) {
          return;
        }
      }
    }
  }
	
  public void shutdownAllServers() {
    Iterator   iter = m_servers.values().iterator();
    ServerInfo info;
    
    while(iter.hasNext()) {
      info = (ServerInfo)iter.next();
      cancelServer(info.getJavaProject());
    }
  }
}

/**
 * 
 * When an L2 is started, one of these is associated with the resulting
 * IProcess via the m_servers map. 
 */
class ServerInfo {
  IJavaProject m_javaProject;
  int          m_jmxPort;
  String       m_name;
  
  ServerInfo(IJavaProject javaProject, String name, int jmxPort) {
    m_javaProject = javaProject;
    m_name        = name;
    m_jmxPort     = jmxPort;
  }
  
  IJavaProject getJavaProject() {
    return m_javaProject;
  }
  
  String getName() {
    return m_name;
  }
  
  int getJmxPort() {
    return m_jmxPort;
  }
}