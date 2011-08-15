/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

public class MoveTypeParticipant extends MoveParticipant {
  private IType        fType;
  private IJavaElement fDestination;

  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }

  public Change createChange(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return createChangesForTypeMove(fType, fDestination);
  }

  public String getName() {
    return "TCTypeChange";
  }

  protected boolean initialize(Object element) {
    fType = (IType)element;

    Object destination = getArguments().getDestination();
    if(destination instanceof IPackageFragment || destination instanceof IType) {
      fDestination = (IJavaElement)destination;
      return true;
    }
    return false;
  }

  public static Change createChangesForTypeMove(IType type, IJavaElement destination) {
    IJavaProject pdestination = destination.getJavaProject();
    String newpname = null;
    if (!type.getJavaProject().equals(pdestination)) {
      newpname = pdestination.getElementName();
    }
    String newfqname = type.getElementName();
    if (destination instanceof IType) {
      newfqname = ((IType)destination).getFullyQualifiedName() + '$' + type.getElementName();
    } 
    else if (destination instanceof IPackageFragment) {
      if (!((IPackageFragment) destination).isDefaultPackage()) {
        newfqname = destination.getElementName() + '.' + type.getElementName();
      }
    } 
    return new MoveTypeChange(type, newfqname, newpname);
  }
}
