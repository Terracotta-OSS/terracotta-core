/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.MultiChangeSignaller;
import org.terracotta.dso.TcPlugin;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class BaseDeleteFieldChange extends Change {
  private IField fField;
    
  public BaseDeleteFieldChange(IField field) {
    super();
    fField = field;
  }
  
  public Object getModifiedElement() {
    return null;
  }
  
  public String getName() {
    return "TCBaseDeleteFieldConfigUpdate";
  }
  
  public void initializeValidationData(IProgressMonitor pm) {/**/}
  
  public RefactoringStatus isValid(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }
  
  public Change perform(IProgressMonitor pm) {
    TcPlugin plugin  = TcPlugin.getDefault();
    IProject project = fField.getJavaProject().getProject();
    TcConfig config  = (TcConfig)plugin.getConfiguration(project).copy();
    MultiChangeSignaller signaller = new MultiChangeSignaller();

    plugin.getConfigurationHelper(project).baseEnsureNotRoot(fField, signaller);
    signaller.signal(project);
    
    // create the undo change
    return new ConfigUndoneChange(project, config);
  }
}
