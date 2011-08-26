/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.TcPlugin;

public class RenamePackageFragmentChange extends Change {
  private String fOldFragmentName;
  private String fNewFragmentName;
  private String fProjectName;
    
  public RenamePackageFragmentChange(IPackageFragment fragment, String newName) {
    fOldFragmentName = fragment.getElementName();
    fNewFragmentName = newName;
    fProjectName     = fragment.getAncestor(IJavaElement.JAVA_PROJECT).getElementName();
  }
  
  public RenamePackageFragmentChange(String oldTypeName,
                                     String newTypeName,
                                     String projectName)
  {
    fNewFragmentName = newTypeName;
    fOldFragmentName = oldTypeName;
    fProjectName     = projectName;
  }

  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject project = workspace.getRoot().getProject(fProjectName);
    IFile configFile = TcPlugin.getDefault().getConfigurationFile(project);
    return "Replace '"+fOldFragmentName+"' with '"+fNewFragmentName+"' in "+configFile.getName();
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
    
    TcPlugin.getDefault().replaceConfigText(project, fOldFragmentName, fNewFragmentName);
    
    // create the undo change
    return new RenamePackageFragmentChange(fNewFragmentName, fOldFragmentName, fProjectName);
  }
}
