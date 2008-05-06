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
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;

public class RenameTypeChange extends Change {
  private String fOldTypeName;
  private String fNewTypeName;
  private String fProjectName;
    
  public RenameTypeChange(IType type, String newTypeName) {
    fOldTypeName = PatternHelper.getFullyQualifiedName(type);
    fNewTypeName = StringUtils.replace(fOldTypeName, type.getElementName(), newTypeName);
    fProjectName = type.getAncestor(IJavaElement.JAVA_PROJECT).getElementName();
  }
  
  public RenameTypeChange(String oldTypeName,
                          String newTypeName,
                          String projectName)
  {
    fNewTypeName = newTypeName;
    fOldTypeName = oldTypeName;
    fProjectName = projectName;
  }

  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCRenameTypeConfigUpdate";
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
    
    TcPlugin.getDefault().replaceConfigText(project, fOldTypeName, fNewTypeName);
    
    // create the undo change
    return new RenameTypeChange(fNewTypeName, fOldTypeName, fProjectName);
  }
}
