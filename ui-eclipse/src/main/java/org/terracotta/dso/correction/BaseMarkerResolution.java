/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ui.IMarkerResolution;

import org.terracotta.dso.TcPlugin;

public abstract class BaseMarkerResolution
  implements IMarkerResolution,
             IWorkspaceRunnable
{
  protected IJavaElement m_element;
  protected IMarker      m_marker;
  
  public BaseMarkerResolution(IJavaElement element) {
    m_element = element;
  }
  
  public void run(IMarker marker) {
    try {
      m_marker = marker;
      m_marker.getResource().getWorkspace().run(this, null, IWorkspace.AVOID_UPDATE, null);
    } catch(CoreException e) {
      e.printStackTrace();
    }
  }

  public ICompilationUnit getCompilationUnit() {
    if(m_element != null) {
      return (ICompilationUnit)m_element.getAncestor(IJavaElement.COMPILATION_UNIT);
    }
    
    return null;
  }

  protected void inspectCompilationUnit() {
    ICompilationUnit cu = getCompilationUnit();
    
    if(cu != null) {
      TcPlugin.getDefault().inspect(cu);
    }
  }
}
