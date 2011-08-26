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
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

public class RenameFieldParticipant extends RenameParticipant {
  private IField fField;
  private String fDestination;

  public RefactoringStatus checkConditions(IProgressMonitor pm,
                                           CheckConditionsContext context)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }

  public Change createChange(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return createChangesForFieldRename(fField, fDestination);
  }

  public String getName() {
    return "TCRenameFieldChange";
  }

  protected boolean initialize(Object element) {
    TcPlugin            plugin       = TcPlugin.getDefault();
    IField              field        = (IField)element;
    IProject            project      = field.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);

    if(configHelper.isRoot(field)) {
      fField       = field;
      fDestination = getArguments().getNewName();
      
      return true;
    }

    return false;
  }

  public static Change createChangesForFieldRename(IField field, String destination) {
    return new RenameFieldChange(field, destination);
  }
}
