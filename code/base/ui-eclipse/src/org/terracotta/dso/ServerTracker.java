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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.actions.ActionUtil;
import org.terracotta.dso.decorator.ServerRunningDecorator;
import org.terracotta.dso.dialogs.AbstractApplicationEventDialog;
import org.terracotta.dso.dialogs.NonPortableObjectDialog;
import org.terracotta.dso.dialogs.ReadOnlyObjectDialog;
import org.terracotta.dso.dialogs.UnlockedSharedObjectDialog;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.TCStop;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.appevent.AbstractApplicationEvent;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.ReadOnlyObjectEvent;
import com.tc.object.appevent.UnlockedSharedObjectEvent;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Used to start a server using the project's config information. Listens for user-initiated termination requests and
 * shuts down the associated server. A mapping is maintained (m_server) between the IProcess and a ServerInfo.
 * ServerInfo contains the IJavaProject and the JMX port of the running server. The JMX port is used for stopping the
 * server and it's done this way to because otherwise we must rely on the configuration, which the user can change.
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
            serverInfo.setStatus(ServerInfo.TERMINATED);
            IJavaProject javaProj = serverInfo.getJavaProject();
            if (!anyRunning(javaProj)) {
              setRunning(javaProj, null);
            }
          }
        }
      }
    }
  }

  public boolean anyRunning(IJavaProject javaProj) {
    Iterator iter = m_servers.keySet().iterator();

    while (iter.hasNext()) {
      IProcess proc = (IProcess) iter.next();
      ServerInfo serverInfo = m_servers.get(proc);
      IJavaProject project = serverInfo.getJavaProject();

      if (project.equals(javaProj)) { return true; }
    }

    return false;
  }

  public ServerInfo getServerInfo(IJavaProject javaProj, String name) {
    Iterator iter = m_servers.keySet().iterator();

    while (iter.hasNext()) {
      IProcess proc = (IProcess) iter.next();
      ServerInfo serverInfo = m_servers.get(proc);
      String serverName = serverInfo.getName();

      if (name.equals(serverName)) { return serverInfo; }
    }

    return null;
  }

  public boolean isRunning(IJavaProject javaProj, String name) {
    ServerInfo serverInfo = getServerInfo(javaProj, name);
    return serverInfo != null && serverInfo.isStarted();
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

  public void startServer(IJavaProject javaProject, String name, IProgressMonitor monitor)
      throws InvocationTargetException {
    if (isRunning(javaProject, name)) {
      internalStopServer(javaProject, true, name, monitor);
    } else {
      try {
        internalStartServer(javaProject, name, monitor);
      } catch (CoreException ce) {
        throw new InvocationTargetException(ce);
      }
    }
  }

  private void internalStartServer(final IJavaProject javaProject, final String name, final IProgressMonitor monitor)
      throws CoreException {
    monitor.beginTask("Starting Terracotta Server '" + name + "' ...", IProgressMonitor.UNKNOWN);

    TcPlugin plugin = TcPlugin.getDefault();
    String projName = javaProject.getElementName();
    int jmxPort = plugin.getJmxPort(javaProject.getProject(), name);
    ILaunch launch = plugin.launchServer(javaProject, projName, name, null);
    String statusMsg = null;

    if (launch != null) {
      ServerInfo info = new ServerInfo(javaProject, name, jmxPort != 0 ? jmxPort : 9520);
      IProcess[] processes = launch.getProcesses();

      if (processes.length > 0) {
        m_servers.put(processes[0], info);

        DebugPlugin.getDefault().addDebugEventListener(this);
        setRunning(javaProject, Boolean.TRUE);

        waitForMBean(javaProject, name, jmxPort > 0 ? jmxPort : 9520);
        while (!info.isTerminated() && info.isStarting()) {
          ThreadUtil.reallySleep(1000);
        }
        if (info.isTerminated()) {
          statusMsg = "Terracotta Server '" + name + "' failed to start.";
        } else if (info.isStarted()) {
          statusMsg = "Terracotta Server '" + name + "' started.";
        }
      }
    }
    monitor.done();

    if (statusMsg != null) {
      final String msg = statusMsg;
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
          if (window instanceof ApplicationWindow) {
            ((ApplicationWindow) window).setStatus(msg);
          }
        }
      });
    }
  }

  static class L2ConnectListener implements ConnectionListener {
    public void handleConnection() {/**/}

    public void handleException() {/**/}
  }

  class DSOAppEventListener implements NotificationListener {
    private boolean fHandlingApplicationEvent;

    public void handleNotification(Notification notification, Object handback) {
      final Object event = notification.getSource();

      if (!fHandlingApplicationEvent) {
        fHandlingApplicationEvent = true;
        if (event instanceof AbstractApplicationEvent) {
          Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              try {
                handleApplicationEvent((AbstractApplicationEvent) event);
              } finally {
                fHandlingApplicationEvent = false;
              }
            }
          });
        }
      }
    }
  }

  private void handleApplicationEvent(AbstractApplicationEvent event) {
    Shell shell = ActionUtil.findSelectedEditorPart().getSite().getShell();
    AbstractApplicationEventDialog dialog = null;

    if (event instanceof NonPortableObjectEvent) {
      dialog = new NonPortableObjectDialog(shell, (NonPortableObjectEvent) event);
    } else if (event instanceof UnlockedSharedObjectEvent) {
      dialog = new UnlockedSharedObjectDialog(shell, (UnlockedSharedObjectEvent) event);
    } else if (event instanceof ReadOnlyObjectEvent) {
      dialog = new ReadOnlyObjectDialog(shell, (ReadOnlyObjectEvent) event);
    }

    if (dialog != null) {
      dialog.open();
    }
  }

  private void waitForMBean(final IJavaProject javaProject, final String name, final int jmxPort) {
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
                ServerInfo serverInfo = getServerInfo(javaProject, name);
                serverInfo.setStatus(ServerInfo.STARTED);
                return;
              }
            }
          } catch (Exception e) {/**/
          }

          ThreadUtil.reallySleep(1000);
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

  public void stopServer(IJavaProject javaProject, IProgressMonitor monitor) throws InvocationTargetException {
    internalStopServer(javaProject, false, null, monitor);
  }

  public void stopServer(IJavaProject javaProject, String name, IProgressMonitor monitor)
      throws InvocationTargetException {
    internalStopServer(javaProject, false, name, monitor);
  }

  private void internalStopServer(IJavaProject javaProject, boolean restart, String name, IProgressMonitor monitor)
      throws InvocationTargetException {
    new TCStopper(javaProject, restart, name).run(monitor);
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
        monitor.beginTask("Stopping Terracotta Server '" + m_name + "' ...", IProgressMonitor.UNKNOWN);
        doStopServer(m_javaProject, m_name, monitor);
        if (m_restart) {
          internalStartServer(m_javaProject, m_name, monitor);
        }
      } catch (Exception e) {
        throw new InvocationTargetException(e);
      }
    }
  }

  private void doStopServer(IJavaProject targetProj, String targetName, IProgressMonitor monitor) throws IOException,
      DebugException {
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

        stopper.stop();

        int count = 0;
        while (true) {
          try {
            proc.getExitValue();
            monitor.done();
            return;
          } catch (DebugException de) {
            if (count++ == 6) {
              proc.terminate();
              monitor.done();
              return;
            }
            ThreadUtil.reallySleep(1000);
          }
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
