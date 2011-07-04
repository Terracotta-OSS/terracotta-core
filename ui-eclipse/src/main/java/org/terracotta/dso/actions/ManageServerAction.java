/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.terracotta.dso.ServerInfo;
import org.terracotta.dso.ServerTracker;
import org.terracotta.dso.TcPlugin;

import com.terracottatech.config.Server;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

/**
 * Manage the named server.
 */

public class ManageServerAction extends BaseAction implements IRunnableWithProgress {
  private Server                 m_server;
  private String                 m_name;
  private static ImageDescriptor m_startImage;
  private static ImageDescriptor m_stopImage;

  static {
    String pluginId = TcPlugin.getPluginId();
    String startPath = "/images/eclipse/start.gif";
    String stopPath = "/images/eclipse/stop.gif";

    m_startImage = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, startPath);
    m_stopImage = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, stopPath);
  }

  private static String buildNameForServer(Server server) {
    if (server == null) {
      server = TcPlugin.DEFAULT_SERVER_INSTANCE;
    }
    String name;
    if (server.isSetName()) {
      name = server.getName();
    } else {
      int dsoPort = server.isSetDsoPort() ? server.getDsoPort().getIntValue() : 9510;
      name = server.getHost() + ":" + dsoPort;
    }
    return name;
  }

  public ManageServerAction(IJavaProject javaProject) {
    this(javaProject, TcPlugin.DEFAULT_SERVER_INSTANCE);
  }

  public ManageServerAction(IJavaProject javaProject, Server server) {
    super(null);

    setJavaElement(javaProject);
    m_server = (Server) server.copy();
    TcPlugin.getDefault().replacePatterns(m_server);
    setText(m_name = buildNameForServer(m_server));

    ServerTracker serverTracker = ServerTracker.getDefault();
    ServerInfo serverInfo = serverTracker.getServerInfo(javaProject, m_name);
    ImageDescriptor imageDesc = serverInfo != null ? m_stopImage : m_startImage;
    setImageDescriptor(imageDesc);
    setEnabled(serverInfo == null || serverInfo.isStarted());
    if (serverInfo != null && serverInfo.isStarting()) {
      setText(m_name + " (Starting...)");
    }
  }

  public void performAction() {
    IWorkbench workbench = PlatformUI.getWorkbench();

    if (!workbench.saveAllEditors(true)) { return; }

    TcPlugin plugin = TcPlugin.getDefault();
    IJavaProject javaProject = (IJavaProject) getJavaElement();
    ServerTracker tracker = ServerTracker.getDefault();
    boolean isRunning = tracker.isRunning(javaProject, m_name);
    IProject project = javaProject.getProject();

    try {
      if (!plugin.continueWithConfigProblems(project)) { return; }
    } catch (CoreException ce) {
      reportError(isRunning, ce);
    }

    try {
      IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      window.run(true, true, this);
    } catch (Exception e) {
      reportError(isRunning, e);
    }
  }

  private static void reportError(boolean isRunning, Exception e) {
    Shell shell = new Shell();
    String msg = MessageFormat.format("Error {0} Terracotta Server instance:\n\n", isRunning ? "stopping" : "starting");
    MessageDialog.openInformation(shell, "Terracotta", msg + ActionUtil.getStatusMessages(e));
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    if (monitor != null && monitor.isCanceled()) throw new InterruptedException("Cancelled server '" + m_name + "'");

    IJavaProject javaProject = (IJavaProject) getJavaElement();
    ServerTracker tracker = ServerTracker.getDefault();

    if (tracker.isRunning(javaProject, m_name)) {
      tracker.stopServer(javaProject, m_name, m_server, monitor);
    } else {
      tracker.startServer(javaProject, m_name, m_server, monitor);
    }
  }
}
