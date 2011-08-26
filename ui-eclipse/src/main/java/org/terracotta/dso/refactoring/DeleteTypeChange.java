/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

import com.terracottatech.config.TcConfigDocument.TcConfig;

public class DeleteTypeChange extends CompositeChange {
  private IType    fType;
  private TcConfig fOrigConfig;

  public DeleteTypeChange(IType type) {
    super("TCDeleteTypeConfigUpdate");

    fType = type;

    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = type.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);
    IField[] fields;
    IType[] types;

    fOrigConfig = (TcConfig) plugin.getConfiguration(project).copy();

    try {
      fields = type.getFields();
      for (int i = 0; i < fields.length; i++) {
        if (configHelper.isRoot(fields[i])) {
          add(new BaseDeleteFieldChange(fields[i]));
        }
      }
    } catch (JavaModelException jme) {
      /**/
    }

    try {
      types = type.getTypes();
      for (int i = 0; i < types.length; i++) {
        add(new BaseDeleteTypeChange(types[i]));
      }
    } catch (JavaModelException jme) {
      /**/
    }
  }

  public Object getModifiedElement() {
    return null;
  }

  public String getName() {
    return "TCDeleteTypeConfigUpdate";
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

    if (configHelper.isAdaptable(fType)) {
      configHelper.baseEnsureNotAdaptable(fType, signaller);
    }
    if (configHelper.isExcluded(fType)) {
      configHelper.baseEnsureNotExcluded(fType, signaller);
    }
    signaller.signal(project);

    return undoChange;
  }

  protected Change createUndoChange(Change[] childUndos) {
    IProject project = fType.getJavaProject().getProject();
    return new ConfigUndoneChange(project, fOrigConfig);
  }
}
