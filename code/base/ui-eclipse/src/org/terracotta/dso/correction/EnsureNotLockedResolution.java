/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

public class EnsureNotLockedResolution extends BaseMarkerResolution {
  public EnsureNotLockedResolution(IMethod method) {
    super(method);
  }
  
  public String getLabel() {
    return "Ensure not locked";
  }

  public void run(IProgressMonitor monitor) throws CoreException {
    IMethod             method       = (IMethod)m_element;
    IProject            project      = method.getJavaProject().getProject();
    ConfigurationHelper configHelper = TcPlugin.getDefault().getConfigurationHelper(project);
    
    configHelper.ensureNotLocked(method);
    m_marker.setAttribute(IMarker.MESSAGE, "No longer locked");
    inspectCompilationUnit();
  }
}

