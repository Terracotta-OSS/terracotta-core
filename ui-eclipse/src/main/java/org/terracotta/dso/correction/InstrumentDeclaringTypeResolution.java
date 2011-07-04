/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

public class InstrumentDeclaringTypeResolution extends BaseMarkerResolution {
  public InstrumentDeclaringTypeResolution(IJavaElement element) {
    super(element);
  }
  
  public String getLabel() {
    return "Instrument declaring type";
  }

  public void run(IProgressMonitor monitor) throws CoreException {
    IType               declaringType = (IType)m_element.getAncestor(IJavaElement.TYPE);
    IProject            project       = declaringType.getJavaProject().getProject();
    ConfigurationHelper configHelper  = TcPlugin.getDefault().getConfigurationHelper(project);
    
    configHelper.ensureAdaptable(declaringType);
    m_marker.setAttribute(IMarker.MESSAGE, "Declaring type instrumented");
    inspectCompilationUnit();
  }
}

