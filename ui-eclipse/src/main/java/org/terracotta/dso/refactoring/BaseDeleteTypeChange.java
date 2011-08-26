/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.MultiChangeSignaller;
import org.terracotta.dso.TcPlugin;

public class BaseDeleteTypeChange extends CompositeChange {
  private IType fType;

  public BaseDeleteTypeChange(IType type) {
    super("TCBaseDeleteTypeConfigUpdate");

    fType = type;

    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = type.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);
    IField[] fields;

    try {
      fields = type.getFields();
      for (int i = 0; i < fields.length; i++) {
        if (configHelper.isRoot(fields[i])) {
          add(new BaseDeleteFieldChange(fields[i]));
        }
      }
    } catch (JavaModelException jme) {/**/
    }
  }

  public Object getModifiedElement() {
    return null;
  }

  public String getName() {
    return "TCBaseDeleteTypeConfigUpdate";
  }

  public void initializeValidationData(IProgressMonitor pm) {/**/}

  public RefactoringStatus isValid(IProgressMonitor pm) throws OperationCanceledException {
    return new RefactoringStatus();
  }

  public Change perform(IProgressMonitor pm) throws CoreException {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = fType.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);
    MultiChangeSignaller signaller = new MultiChangeSignaller();

    Change undoChange = super.perform(pm);

    configHelper.baseEnsureNotAdaptable(fType, signaller);
    configHelper.baseEnsureNotExcluded(fType, signaller);

    // create the undo change
    return undoChange;
  }
}
