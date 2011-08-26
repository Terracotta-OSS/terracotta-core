/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;

public class RenameMethodChange extends Change {
  private String fOldMethodName;
  private String fNewMethodName;
  private String fProjectName;
    
  public RenameMethodChange(IMethod method, String newMethodName) {
    fOldMethodName = PatternHelper.getFullName(method);
    fNewMethodName = StringUtils.replace(fOldMethodName, method.getElementName(), newMethodName);
    fProjectName   = method.getAncestor(IJavaElement.JAVA_PROJECT).getElementName();
  }
  
  public RenameMethodChange(String oldMethodName,
                           String newMethodName,
                           String projectName)
  {
    fNewMethodName = newMethodName;
    fOldMethodName = oldMethodName;
    fProjectName   = projectName;
  }

  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCRenameMethodConfigUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {  
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject   project   = workspace.getRoot().getProject(fProjectName);
    
    TcPlugin.getDefault().replaceConfigText(project, fOldMethodName, fNewMethodName);
    
    // create the undo change
    return new RenameMethodChange(fNewMethodName, fOldMethodName, fProjectName);
  }
}
