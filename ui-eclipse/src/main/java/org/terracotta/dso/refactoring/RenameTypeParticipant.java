/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class RenameTypeParticipant extends RenameParticipant {
  private IType  fType;
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
    return createChangesForTypeRename(fType, fDestination);
  }

  public String getName() {
    return "TCRenameTypeChange";
  }

  protected boolean initialize(Object element) {
    fType        = (IType)element;
    fDestination = getArguments().getNewName();
    
    return true;
  }

  public static Change createChangesForTypeRename(IType type, String destination) {
    return new RenameTypeChange(type, destination);
  }
}
