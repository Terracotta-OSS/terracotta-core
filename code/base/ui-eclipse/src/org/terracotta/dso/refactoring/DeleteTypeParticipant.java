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
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

public class DeleteTypeParticipant extends DeleteParticipant {
  private IType fType;

  public RefactoringStatus checkConditions(IProgressMonitor pm,
                                           CheckConditionsContext context)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }

  public Change createChange(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return createChangesForTypeDelete(fType);
  }

  public String getName() {
    return "TCDeleteTypeChange";
  }

  protected boolean initialize(Object element) {
    fType = (IType)element;
    return true;
  }

  public static Change createChangesForTypeDelete(IType type) {
    return new DeleteTypeChange(type);
  }
}
