/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.apache.commons.lang.StringUtils;
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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.actions.ManageServerAction;

import com.tc.server.ServerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Invoked when a project resource change occurs.  First test to see if
 * the config file content changed and, if so, clear the config session
 * information.  Next check if a module has been compiled and, if so, inspect
 * the module for terracotta artifacts.
 * 
 * @see org.eclipse.core.resources.IResourceDeltaVisitor
 * @see org.eclipse.ui.IWorkbench.addResourceChangeListener
 * @see TcPlugin.ResourceListener
 */

public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
  private static final boolean debug =
    Boolean.getBoolean("ResourceDeltaVisitor.debug");
  
  boolean fIgnoreNextConfigChange;
  
  public boolean visit(IResourceDelta delta) {
    final TcPlugin  plugin  = TcPlugin.getDefault();
    int             kind    = delta.getKind();
    int             flags   = delta.getFlags();
    IResource       res     = delta.getResource();
    final IProject  project = res.getProject();
    
    if(debug) {
      dump(delta);
    }
    
    if(!plugin.hasTerracottaNature(project)) {
      return true;
    }
    
    switch(kind) {
      case IResourceDelta.CHANGED: {
        if((flags & IResourceDelta.CONTENT) != 0) {
          if(res instanceof IFile) {
            IFile  file = (IFile)res;
            IPath  path = file.getLocation();
            String ext  = path.getFileExtension();
            
            if(ext.equals("xml")) {
              if((flags & IResourceDelta.MARKERS) != 0) {
                return false;
              }
              if(plugin.getConfigurationFile(project).equals(res)) {
                final boolean wasIgnoringChange = fIgnoreNextConfigChange;
                if(fIgnoreNextConfigChange) {
                  fIgnoreNextConfigChange = false;
                }
                try {
                  if(!wasIgnoringChange) {
                    new Job("Reloading DSO Configuration") {
                      public IStatus run(IProgressMonitor monitor) {
                        plugin.reloadConfiguration(project);
                        return Status.OK_STATUS;
                      }
                    }.schedule();
                  }
                  new Job("Restart?") {
                    public IStatus run(IProgressMonitor monitor) {
                      queryRelaunchAll(project);
                      return Status.OK_STATUS;
                    }
                  }.schedule();
                } catch(Exception e) {/**/}
                return false;
              }
            }
          }
        }
        break;
      }
      case IResourceDelta.ADDED: {
        if((flags & IResourceDelta.MOVED_FROM) != 0) {        
          if(res instanceof IFile) {
            plugin.fileMoved((IFile)res, delta.getMovedFromPath());
          }
        }
        else if(res instanceof IProject) {
          IProject aProject = (IProject)res;
          
          if(plugin.getConfigurationFile(aProject) == null) {
            plugin.staleProjectAdded(aProject);
          }
        }
        break;
      }
      case IResourceDelta.REMOVED: {
        if((flags & IResourceDelta.MOVED_TO) == 0) {        
          if(res instanceof IFile) {
            plugin.fileRemoved((IFile)res);
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
    final Map<String, ILaunch> serverLaunches = new HashMap<String, ILaunch>();
    
    for (ILaunch launch : launchManager.getLaunches()) {
      if (launch.isTerminated()) continue;
      ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
      try {
        String mainClass = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
        if(mainClass.equals(ServerConstants.SERVER_MAIN_CLASS_NAME)) {
          String serverLaunchName = launchConfig.getName();
          String[] launchNameElems = StringUtils.split(serverLaunchName, ".");
          String projName = launchNameElems[0];
          String serverName = launchNameElems[1];
          if(projName.equals(project.getName())) {
            serverLaunches.put(serverName, launch);
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

    if(launches.size() > 0 || serverLaunches.size() > 0) {
      Display display = Display.getDefault();
      display.syncExec(new Runnable() {
        public void run() {
          Shell shell = Display.getCurrent().getActiveShell();
          String title = "Terracotta";
          String msg = "The configuration file changed. Relaunch all related launch targets?";
          if(MessageDialog.openQuestion(shell, title, msg)) {
            new Job("Restarting Terracotta") {
              public IStatus run(IProgressMonitor monitor) {
                if(monitor == null) monitor = new NullProgressMonitor();
                
                ArrayList<ILaunch> allLaunches = new ArrayList<ILaunch>(launches);
                allLaunches.addAll(serverLaunches.values());
                for(ILaunch launch : allLaunches) {
                  safeTerminateLaunch(launch);
                }

                for(Iterator<String> iter = serverLaunches.keySet().iterator(); iter.hasNext();) {
                  try {
                    ManageServerAction msa = new ManageServerAction(JavaCore.create(project), iter.next());
                    msa.performAction();
                  } catch(Exception e) {
                    e.printStackTrace();
                  }
                }
                for(ILaunch launch : launches) {
                  ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
                  try {
                    launchConfig.launch(launch.getLaunchMode(), monitor);
                  } catch(CoreException ce) {
                    ce.printStackTrace();
                  }
                }
                return Status.OK_STATUS;
              }
            }.schedule();
          }
        }
      });
    }
  }
  
  private static void safeTerminateLaunch(ILaunch launch) {
    try {
      launch.terminate();
    } catch(DebugException de) {
      /**/
    }
  }
  
  private void dump(IResourceDelta delta) {
    int          kind  = delta.getKind();
    int          flags = delta.getFlags();
    StringBuffer sb    = new StringBuffer();
    
    sb.append(delta.getResource().getFullPath());
    
    switch(kind) {
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
    
    if((flags & IResourceDelta.CONTENT) != 0) {
      sb.append(" CONTENT");
    }
    if((flags & IResourceDelta.MOVED_FROM) != 0) {
      sb.append(" MOVED_FROM");
    }
    if((flags & IResourceDelta.MOVED_TO) != 0) {
      sb.append(" MOVED_TO");
    }
    if((flags & IResourceDelta.OPEN) != 0) {
      sb.append(" OPEN");
    }
    if((flags & IResourceDelta.TYPE) != 0) {
      sb.append(" TYPE");
    }
    if((flags & IResourceDelta.SYNC) != 0) {
      sb.append(" SYNC");
    }
    if((flags & IResourceDelta.MARKERS) != 0) {
      sb.append(" MARKERS");
    }
    if((flags & IResourceDelta.REPLACED) != 0) {
      sb.append(" REPLACED");
    }
    if((flags & IResourceDelta.DESCRIPTION) != 0) {
      sb.append(" DESCRIPTION");
    }
    if((flags & IResourceDelta.ENCODING) != 0) {
      sb.append(" ENCODING");
    }
    
    System.out.println(sb.toString());
  }
}
