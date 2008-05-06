/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;
import com.tc.aspectwerkz.reflect.MethodInfo;

public class DeleteMethodParticipant extends DeleteParticipant {
  private IMethod fMethod;

  public RefactoringStatus checkConditions(IProgressMonitor pm,
                                           CheckConditionsContext context)
    throws OperationCanceledException
  {
    return new RefactoringStatus();
  }

  public Change createChange(IProgressMonitor pm)
    throws OperationCanceledException
  {
    return createChangesForMethodDelete(fMethod);
  }

  public String getName() {
    return "TCDeleteMethodChange";
  }

  protected boolean initialize(Object element) {
    TcPlugin            plugin       = TcPlugin.getDefault();
    IMethod             method       = (IMethod)element;
    IProject            project      = method.getJavaProject().getProject();
    ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);

    if(configHelper.isLocked(method)) {
      fMethod = method;
      return true;
    }

    return false;
  }

  public static Change createChangesForMethodDelete(IMethod method) {
    MethodInfo methodInfo = PatternHelper.getHelper().getMethodInfo(method);
    return new DeleteMethodChange(method, methodInfo);
  }
}
