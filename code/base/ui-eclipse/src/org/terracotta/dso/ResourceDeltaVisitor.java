/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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
                if(fIgnoreNextConfigChange) {
                  fIgnoreNextConfigChange = false;
                } else {
                  try {
                    new Job("Reloading DSO Configuration") {
                      public IStatus run(IProgressMonitor monitor) {
                        plugin.reloadConfiguration(project);
                        return Status.OK_STATUS;
                      }
                    }.schedule();
                  } catch(Exception e) {/**/}
                }
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
