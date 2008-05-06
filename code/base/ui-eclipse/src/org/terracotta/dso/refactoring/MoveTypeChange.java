/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.TcPlugin;

public class MoveTypeChange extends Change {
  private String fNewTypeName;
  private String fNewProjectName;
  private String fOldTypeName;
  private String fOldProjectName;
  
  public MoveTypeChange(IType srcType, String newTypeName, String newProjectName) {
    this(srcType.getFullyQualifiedName(),
         srcType.getAncestor(IJavaElement.JAVA_PROJECT).getElementName(),
         newTypeName,
         newProjectName);
  }
  
  public MoveTypeChange(String oldTypeName,
                        String oldProjectName,
                        String newTypeName,
                        String newProjectName)
  {
    fNewTypeName    = newTypeName;
    fNewProjectName = newProjectName;
    fOldTypeName    = oldTypeName;
    fOldProjectName = oldProjectName;
  }

  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCMoveTypeConfigUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {  
    IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
    IProject   project   = workspace.getRoot().getProject(fOldProjectName);
    
    TcPlugin.getDefault().replaceConfigText(project, fOldTypeName, fNewTypeName);
    
    // create the undo change
    return new MoveTypeChange(fNewTypeName, fNewProjectName, fOldTypeName, fOldProjectName);
  }
}
