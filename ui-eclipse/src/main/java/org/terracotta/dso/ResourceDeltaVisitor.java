/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.actions.BuildBootJarAction;
import org.terracotta.dso.actions.ManageServerAction;
import org.terracotta.dso.dialogs.RelaunchDialog;

import com.tc.server.ServerConstants;
import com.terracottatech.config.Server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Invoked when a project resource change occurs. First test to see if the config file content changed and, if so, clear
 * the config session information. Next check if a module has been compiled and, if so, inspect the module for
 * terracotta artifacts.
 * 
 * @see org.eclipse.core.resources.IResourceDeltaVisitor
 * @see org.eclipse.ui.IWorkbench.addResourceChangeListener
 * @see TcPlugin.ResourceListener
 */

public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
  private static final boolean debug = Boolean.getBoolean("ResourceDeltaVisitor.debug");

  boolean                      fIgnoreNextConfigChange;

  public boolean visit(IResourceDelta delta) {
    final TcPlugin plugin = TcPlugin.getDefault();
    int kind = delta.getKind();
    int flags = delta.getFlags();
    IResource res = delta.getResource();
    final IProject project = res.getProject();

    if (debug) {
      dump(delta);
    }

    if (plugin == null || !plugin.hasTerracottaNature(project)) { return true; }

    switch (kind) {
      case IResourceDelta.CHANGED: {
        if ((flags & IResourceDelta.MARKERS) != 0) { return false; }

        if ((flags & IResourceDelta.CONTENT) != 0) {
          if (res instanceof IFile) {
            IFile file = (IFile) res;
            IPath path = file.getLocation();
            String ext = path.getFileExtension();
            int segmentCount = path.segmentCount();

            if (segmentCount == 2 && path.segment(segmentCount - 1).equals(".classpath")) {
              final IJavaProject javaProject = JavaCore.create(project);

              if (BootClassHelper.canGetBootTypes(javaProject)) {
                plugin.setBootClassHelper(project, new BootClassHelper(javaProject));
              } else {
                Job job = new Job("Building bootjar") {
                  public IStatus run(IProgressMonitor monitor) {
                    try {
                      new BuildBootJarAction(javaProject).run(monitor);
                    } catch (Exception e) {
                      plugin.openError("Building bootjar", e);
                    }
                    return Status.OK_STATUS;
                  }
                };
                job.schedule();
              }
              return false;
            }

            if (ext.equals("xml")) {
              if (plugin.getConfigurationFile(project).equals(res)) {
                final boolean queryRestart = plugin.getQueryRestartOption(project);
                final boolean wasIgnoringChange = fIgnoreNextConfigChange;
                if (fIgnoreNextConfigChange) {
                  fIgnoreNextConfigChange = false;
                }
                try {
                  if (!wasIgnoringChange) {
                    Job job = new Job("Reloading DSO Configuration") {
                      public IStatus run(IProgressMonitor monitor) {
                        plugin.reloadConfiguration(project);
                        if (queryRestart) {
                          queryRelaunchAll(project);
                        }
                        return Status.OK_STATUS;
                      }
                    };
                    job.schedule();
                  } else if (queryRestart) {
                    Job job = new Job("Restart?") {
                      public IStatus run(IProgressMonitor monitor) {
                        queryRelaunchAll(project);
                        return Status.OK_STATUS;
                      }
                    };
                    job.schedule();
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return false;
              }
            }
          }
        }
        break;
      }
      case IResourceDelta.ADDED: {
        if ((flags & IResourceDelta.MOVED_FROM) != 0) {
          if (res instanceof IFile) {
            plugin.fileMoved((IFile) res, delta.getMovedFromPath());
          }
        } else if (res instanceof IProject) {
          plugin.validateConfigurationFile(project);
        }
        break;
      }
      case IResourceDelta.REMOVED: {
        if ((flags & IResourceDelta.MOVED_TO) == 0) {
          if (res instanceof IFile) {
            plugin.fileRemoved((IFile) res);
          }
        }
        break;
      }
    }

    return true;
  }

  private void queryRelaunchAll(final IProject project) {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    final List<ILaunch> launches = new ArrayList<ILaunch>();
    final List<ILaunch> serverLaunches = new ArrayList<ILaunch>();

    for (ILaunch launch : launchManager.getLaunches()) {
      IProcess[] processes = launch.getProcesses();
      if (launch.isTerminated() || processes == null || processes.length == 0) continue;
      ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
      try {
        String mainClass = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                                                     (String) null);
        if (mainClass != null && mainClass.equals(ServerConstants.SERVER_MAIN_CLASS_NAME)) {
          String projName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
          if (projName.equals(project.getName())) {
            serverLaunches.add(launch);
          }
        } else {
          ILaunchConfigurationType launchConfigType = launchConfig.getType();
          String id = launchConfigType.getIdentifier();
          if (id.equals("launch.configurationDelegate")) {
            String projName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                                                        (String) null);
            if (projName != null && projName.equals(project.getName())) {
              launches.add(launch);
            }
          }
        }
      } catch (CoreException ce) {/**/
      }
    }

    if (launches.size() > 0 || serverLaunches.size() > 0) {
      final Display display = Display.getDefault();
      display.syncExec(new Runnable() {
        public void run() {
          final Shell shell = TcPlugin.getActiveWorkbenchShell();
          RelaunchDialog relaunchDialog = new RelaunchDialog(shell, project, serverLaunches, launches);
          final int returnCode = relaunchDialog.open();
          if (returnCode == RelaunchDialog.CONTINUE_ID) { return; }
          new Job("Restarting Terracotta") {
            public IStatus run(IProgressMonitor monitor) {
              if (monitor == null) {
                monitor = new NullProgressMonitor();
              }

              ArrayList<ILaunch> allLaunches = new ArrayList<ILaunch>(launches);
              allLaunches.addAll(serverLaunches);
              for (ILaunch launch : allLaunches) {
                safeTerminateLaunch(launch);
              }

              if (returnCode == RelaunchDialog.TERMINATE_ID) { return Status.OK_STATUS; }

              for (Iterator<ILaunch> iter = serverLaunches.iterator(); iter.hasNext();) {
                try {
                  ILaunch launch = iter.next();
                  Server server = TcPlugin.getDefault().getLaunchedServer(project, launch);
                  if (server != null) {
                    ManageServerAction msa = new ManageServerAction(JavaCore.create(project), server);
                    msa.run(monitor);
                  } else {
                    final String launchName = launch.getLaunchConfiguration().getName();
                    display.syncExec(new Runnable() {
                      public void run() {
                        String msg = "Unable to locate config element for server '" + launchName
                                     + ".'  Cancelling client restart.";
                        MessageDialog.openInformation(shell, "Terracotta", msg);
                      }
                    });
                    return Status.OK_STATUS;
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              for (ILaunch launch : launches) {
                DebugUITools.launch(launch.getLaunchConfiguration(), launch.getLaunchMode());
              }
              return Status.OK_STATUS;
            }
          }.schedule();
        }
      });
    }
  }

  private static void safeTerminateLaunch(ILaunch launch) {
    try {
      launch.terminate();
    } catch (DebugException de) {
      /**/
    }
  }

  private void dump(IResourceDelta delta) {
    int kind = delta.getKind();
    int flags = delta.getFlags();
    StringBuffer sb = new StringBuffer();

    sb.append(delta.getResource().getFullPath());

    switch (kind) {
      case IResourceDelta.NO_CHANGE:
        sb.append(" NO_CHANGE");
        break;
      case IResourceDelta.ADDED:
        sb.append(" ADDED");
        break;
      case IResourceDelta.REMOVED:
        sb.append(" REMOVED");
        break;
      case IResourceDelta.CHANGED:
        sb.append(" CHANGED");
        break;
      case IResourceDelta.ADDED_PHANTOM:
        sb.append(" ADDED_PHANTOM");
        break;
      case IResourceDelta.REMOVED_PHANTOM:
        sb.append(" REMOVED_PHANTOM");
        break;
    }

    if ((flags & IResourceDelta.CONTENT) != 0) {
      sb.append(" CONTENT");
    }
    if ((flags & IResourceDelta.MOVED_FROM) != 0) {
      sb.append(" MOVED_FROM");
    }
    if ((flags & IResourceDelta.MOVED_TO) != 0) {
      sb.append(" MOVED_TO");
    }
    if ((flags & IResourceDelta.OPEN) != 0) {
      sb.append(" OPEN");
    }
    if ((flags & IResourceDelta.TYPE) != 0) {
      sb.append(" TYPE");
    }
    if ((flags & IResourceDelta.SYNC) != 0) {
      sb.append(" SYNC");
    }
    if ((flags & IResourceDelta.MARKERS) != 0) {
      sb.append(" MARKERS");
    }
    if ((flags & IResourceDelta.REPLACED) != 0) {
      sb.append(" REPLACED");
    }
    if ((flags & IResourceDelta.DESCRIPTION) != 0) {
      sb.append(" DESCRIPTION");
    }
    if ((flags & IResourceDelta.ENCODING) != 0) {
      sb.append(" ENCODING");
    }

    System.out.println(sb.toString());
  }
}
