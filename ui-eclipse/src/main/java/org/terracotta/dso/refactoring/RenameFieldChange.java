/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;

public class RenameFieldChange extends Change {
  private String fNewFieldName;
  private String fOldFieldName;
  private String fProjectName;
    
  public RenameFieldChange(IField field, String newTypeName) {
    IType  type       = field.getDeclaringType();
    String parentType = PatternHelper.getFullyQualifiedName(type);
    String fieldName  = field.getElementName();
    
    fOldFieldName = parentType+"."+fieldName;
    fNewFieldName = parentType+"."+newTypeName;
    fProjectName  = field.getAncestor(IJavaElement.JAVA_PROJECT).getElementName();
  }
  
  public RenameFieldChange(String oldFieldName,
                           String newFieldName,
                           String projectName)
  {
    fNewFieldName = newFieldName;
    fOldFieldName = oldFieldName;
    fProjectName  = projectName;
  }

  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCRenameFieldConfigUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {  
    TcPlugin   plugin    = TcPlugin.getDefault(); 
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject   project   = workspace.getRoot().getProject(fProjectName);
    
    plugin.getConfigurationHelper(project).renameRoot(fOldFieldName, fNewFieldName);
    
    // create the undo change
    return new RenameFieldChange(fNewFieldName, fOldFieldName, fProjectName);
  }
}
