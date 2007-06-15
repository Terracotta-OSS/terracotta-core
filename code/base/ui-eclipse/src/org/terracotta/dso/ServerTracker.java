/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.dso.decorator.ServerRunningDecorator;
import org.terracotta.dso.dialogs.NonPortableObjectDialog;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.TCStop;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.appevent.NonPortableObjectEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Used to start a server using the project's config information. Listens for user-initiated termination requests and
 * shuts down the associated server. A JavaProject can only have a single server running at one time. A mapping is
 * maintained (m_server) between the IProcess and a ServerInfo. ServerInfo contains the IJavaProject and the JMX port of
 * the running server. The JMX port is used for stopping the server and it's done this way to because otherwise we must
 * rely on the configuration, which the user can change.
 * 
 * @see TcPlugin.launchServer
 */

public class ServerTracker implements IDebugEventSetListener {
  private static ServerTracker          m_instance          = new ServerTracker();
  private HashMap<IProcess, ServerInfo> m_servers           = new HashMap<IProcess, ServerInfo>();

  /**
   * This name is used in plugin.xml for managing the start/stop menu items.
   */
  private static final QualifiedName    SERVER_RUNNING_NAME = new QualifiedName("org.terracotta.dso", "ServerRunning");

  private ServerTracker() {
    super();
  }

  public static synchronized ServerTracker getDefault() {
    if (m_instance == null) {
      m_instance = new ServerTracker();
    }
    return m_instance;
  }

  public void handleDebugEvents(DebugEvent[] events) {
    if (events != null && events.length > 0) {
      if (events[0].getKind() == DebugEvent.TERMINATE) {
        Object source = events[0].getSource();

        if (source instanceof IProcess) {
          ServerInfo serverInfo = m_servers.get(source);

          if (serverInfo != null) {
            m_servers.remove(source);
            setRunning(serverInfo.getJavaProject(), null);
          }
        }
      }
    }
  }

  public boolean anyRunning(IJavaProject javaProj) {
    try {
      return javaProj.getProject().getSessionProperty(SERVER_RUNNING_NAME) != null;
    } catch (CoreException ce) {
      return false;
    }
  }

  public boolean isRunning(IJavaProject javaProj, String name) {
    if (anyRunning(javaProj)) {
      Iterator iter = m_servers.keySet().iterator();
      IProcess proc;
      ServerInfo serverInfo;
      String serverName;

      while (iter.hasNext()) {
        proc = (IProcess) iter.next();
        serverInfo = m_servers.get(proc);
        serverName = serverInfo.getName();

        if (name.equals(serverName)) { return true; }
      }
    }

    return false;
  }

  public void setRunning(IJavaProject javaProj, Boolean value) {
    if (value != null && value.equals(Boolean.FALSE)) {
      value = null;
    }

    IProject project = javaProj.getProject();

    if (project.isOpen()) {
      try {
        project.setSessionProperty(SERVER_RUNNING_NAME, value);
        ServerRunningDecorator.updateDecorators();
      } catch (CoreException ce) {/**/
      }
    }
  }

  public void startServer(IJavaProject javaProject, String name) throws CoreException {
    if (isRunning(javaProject, name)) {
      internalStopServer(javaProject, true, name);
    } else {
      internalStartServer(javaProject, name);
    }
  }

  private void internalStartServer(IJavaProject javaProject, String name) throws CoreException {
    TcPlugin plugin = TcPlugin.getDefault();
    String projName = javaProject.getElementName();
    int jmxPort = plugin.getJmxPort(javaProject.getProject(), name);
    ILaunch launch = plugin.launchServer(javaProject, projName, name);

    if (launch != null) {
      ServerInfo info = new ServerInfo(javaProject, name, jmxPort != 0 ? jmxPort : 9520);
      IProcess[] processes = launch.getProcesses();

      if (processes.length > 0) {
        m_servers.put(processes[0], info);

        DebugPlugin.getDefault().addDebugEventListener(this);
        setRunning(javaProject, Boolean.TRUE);

        waitForMBean(jmxPort > 0 ? jmxPort : 9520);
      }
    }
  }

  static class L2ConnectListener implements ConnectionListener {
    public void handleConnection() {/**/}

    public void handleException() {/**/}
  }

  class DSOAppEventListener implements NotificationListener {
    private boolean fHandlingNonPortableEvent;
    
    public void handleNotification(Notification notification, Object handback) {
      final Object event = notification.getSource();

      if (event instanceof NonPortableObjectEvent && !fHandlingNonPortableEvent) {
        fHandlingNonPortableEvent = true;
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            try {
              handleNonPortableReason((NonPortableObjectEvent) event);
            } finally {
              fHandlingNonPortableEvent = false;
            }
          }
        });
      }
    }
  }

  private void handleNonPortableReason(NonPortableObjectEvent event) {
    Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
    NonPortableObjectDialog dialog = new NonPortableObjectDialog(shell, event);
    dialog.open();
  }

  private void waitForMBean(final int jmxPort) {
    new Thread() {
      public void run() {
        ServerConnectionManager connectManager = new ServerConnectionManager("localhost", jmxPort, false,
            new L2ConnectListener());

        while (true) {
          try {
            if (connectManager.testIsConnected()) {
              ConnectionContext cc = connectManager.getConnectionContext();
              ObjectName on = cc.queryName(L2MBeanNames.DSO_APP_EVENTS.getCanonicalName());

              if (on != null) {
                new NonPortableObjectEvent(null, null);
                cc.addNotificationListener(on, new DSOAppEventListener());
                return;
              }
            }
          } catch (Exception e) {/**/
          }

          try {
            sleep(500);
          } catch (InterruptedException ie) {/**/
          }
        }
      }
    }.start();
  }

  public void cancelServer(IJavaProject javaProject) {
    Iterator iter = m_servers.keySet().iterator();
    IProcess proc;
    ServerInfo info;

    while (iter.hasNext()) {
      proc = (IProcess) iter.next();
      info = m_servers.get(proc);

      if (info.getJavaProject().equals(javaProject)) {
        try {
          proc.terminate();
        } catch (DebugException de) {
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
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    Shell shell = window != null ? window.getShell() : null;

    try {
      IRunnableWithProgress op = new TCStopper(javaProject, restart, name);
      new ProgressMonitorDialog(shell).run(true, false, op);
    } catch (InvocationTargetException e) {
      TcPlugin.getDefault().openError("Cannot stop Terracotta server", e.getCause());
    } catch (InterruptedException e) {/**/
    }
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
      m_restart = restart;
      m_name = name;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException {
      try {
        monitor.beginTask("Stopping Terracotta Server...", IProgressMonitor.UNKNOWN);
        doStopServer(m_javaProject, m_name, monitor);
        if (m_restart) {
          internalStartServer(m_javaProject, m_name);
        }
      } catch (Exception e) {
        throw new InvocationTargetException(e);
      }
    }
  }

  private void doStopServer(IJavaProject targetProj, String targetName, IProgressMonitor monitor) {
    Iterator iter = m_servers.keySet().iterator();
    IProcess proc;
    ServerInfo serverInfo;
    IJavaProject javaProject;
    int jmxPort;
    String name;

    while (iter.hasNext()) {
      proc = (IProcess) iter.next();
      serverInfo = m_servers.get(proc);
      javaProject = serverInfo.getJavaProject();
      jmxPort = serverInfo.getJmxPort();
      name = serverInfo.getName();

      if (javaProject.getProject().isOpen() && targetProj.equals(javaProject) && targetName.equals(name)) {
        TCStop stopper = new TCStop("localhost", jmxPort != -1 ? jmxPort : 9520);

        try {
          stopper.stop();

          int count = 0;
          while (true) {
            try {
              proc.getExitValue();
              iter.remove();
              return;
            } catch (DebugException de) {
              try {
                if (count++ == 6) {
                  proc.terminate();
                  iter.remove();
                  return;
                }
                Thread.sleep(1000);
              } catch (InterruptedException ie) {/**/
              }
            }
          }
        } catch (Exception e) {
          return;
        }
      }
    }
  }

  public void shutdownAllServers() {
    Iterator iter = m_servers.values().iterator();
    ServerInfo info;

    while (iter.hasNext()) {
      info = (ServerInfo) iter.next();
      cancelServer(info.getJavaProject());
    }
  }
}

/**
 * When an L2 is started, one of these is associated with the resulting IProcess via the m_servers map.
 */
class ServerInfo {
  IJavaProject m_javaProject;
  int          m_jmxPort;
  String       m_name;

  ServerInfo(IJavaProject javaProject, String name, int jmxPort) {
    m_javaProject = javaProject;
    m_name = name;
    m_jmxPort = jmxPort;
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

  public String toString() {
    return m_name + ":" + m_jmxPort;
  }
}
