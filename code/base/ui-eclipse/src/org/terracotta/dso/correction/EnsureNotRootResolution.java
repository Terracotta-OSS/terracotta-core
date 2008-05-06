/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

public class EnsureNotRootResolution extends BaseMarkerResolution {
  public EnsureNotRootResolution(IField field) {
    super(field);
  }
  
  public String getLabel() {
    return "Ensure not a root";
  }

  public void run(IProgressMonitor monitor) throws CoreException {
    IField              field        = (IField)m_element;
    IProject            project      = field.getJavaProject().getProject();
    ConfigurationHelper configHelper = TcPlugin.getDefault().getConfigurationHelper(project);
    
    configHelper.ensureNotRoot(field);
    m_marker.setAttribute(IMarker.MESSAGE, "No longer a root");
    inspectCompilationUnit();
  }
}
