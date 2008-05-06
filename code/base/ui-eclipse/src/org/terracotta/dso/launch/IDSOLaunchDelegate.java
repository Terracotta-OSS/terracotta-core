/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;

public interface IDSOLaunchDelegate {
  IJavaProject getJavaProject(ILaunchConfiguration launchConfig) throws CoreException;
  IVMInstall getVMInstall(ILaunchConfiguration launchConfig) throws CoreException;
}
