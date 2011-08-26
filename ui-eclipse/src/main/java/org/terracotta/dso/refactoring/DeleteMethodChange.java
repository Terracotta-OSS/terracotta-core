/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.TcPlugin;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class DeleteMethodChange extends Change {
  private IMethod    fMethod;
  private MethodInfo fMethodInfo;
    
  public DeleteMethodChange(IMethod method, MethodInfo methodInfo) {
    super();
    
    fMethod     = method;
    fMethodInfo = methodInfo;
  }
  
  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCDeleteMethodConfigUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {
    TcPlugin plugin  = TcPlugin.getDefault();
    IProject project = fMethod.getJavaProject().getProject();
    TcConfig config  = (TcConfig)plugin.getConfiguration(project).copy();
    
    plugin.getConfigurationHelper(project).ensureNotLocked(fMethodInfo);
    
    // create the undo change
    return new ConfigUndoneChange(project, config);
  }
}
