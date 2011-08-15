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
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

public class DeleteFieldParticipant extends DeleteParticipant {
  private IField fField;

  public RefactoringStatus checkConditions(IProgressMonitor pm,
                                           CheckConditionsContext context)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }

  public Change createChange(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return createChangesForFieldDelete(fField);
  }

  public String getName() {
    return "TCDeleteFieldChange";
  }

  protected boolean initialize(Object element) {
    TcPlugin            plugin       = TcPlugin.getDefault();
    IField              field        = (IField)element;
    IProject            project      = field.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);

    if(configHelper.isRoot(field)) {
      fField = field;
      return true;
    }

    return false;
  }

  public static Change createChangesForFieldDelete(IField field) {
    return new DeleteFieldChange(field);
  }
}
