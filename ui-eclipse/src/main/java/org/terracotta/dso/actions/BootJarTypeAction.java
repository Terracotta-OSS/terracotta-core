/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * This is currently not used.
 */

/**
 * Marks the currently selected IType as a BootJar class.
 * 
 * @see org.eclipse.jdt.core.IType
 * @see org.terracotta.dso.ConfigurationHelper.isBootJarClass
 * @see org.terracotta.dso.ConfigurationHelper.ensureBootJarClass
 * @see org.terracotta.dso.ConfigurationHelper.ensureNotBootJarClass
 */

public class BootJarTypeAction extends BaseAction {
  private IType                           m_type;
  private final SystemTypeSearchRequestor searchRequestor    = new SystemTypeSearchRequestor();
  private final SearchParticipant[]       searchParticipants = new SearchParticipant[] { SearchEngine
                                                                 .getDefaultSearchParticipant() };

  public BootJarTypeAction() {
    super("Boot Jar", AS_CHECK_BOX);
  }

  private class SystemTypeSearchRequestor extends SearchRequestor {
    private Menu fMenu;

    public void acceptSearchMatch(SearchMatch match) {
      Object element = match.getElement();
      if (element instanceof IType) {
        IType type = (IType) element;
        setJavaElement(m_type = type);
        boolean isBootClass = TcPlugin.getDefault().isBootClass(type.getJavaProject().getProject(), type);
        setEnabled(!isBootClass);
        setChecked(isBootClass || getConfigHelper().isBootJarClass(type));

        ActionContributionItem item = new ActionContributionItem(BootJarTypeAction.this);
        item.fill(fMenu, -1);
      }
    }

    void setMenu(Menu menu) {
      fMenu = menu;
    }
  }

  private void determineSystemType(IType type, Menu menu) {
    int filter = IJavaSearchScope.SYSTEM_LIBRARIES;
    IJavaElement[] elements = new IJavaElement[] { type.getJavaProject() };
    IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, filter);
    SearchPattern pattern = SearchPattern.createPattern(type, IJavaSearchConstants.DECLARATIONS);
    SearchEngine searchEngine = new SearchEngine();
    try {
      searchRequestor.setMenu(menu);
      searchEngine.search(pattern, searchParticipants, scope, searchRequestor, null);
    } catch (CoreException ce) {
      setChecked(false);
    }
  }

  public void setType(IType type, Menu menu) {
    determineSystemType(type, menu);

    // setJavaElement(m_type = type);
    //
    // boolean isBootClass = TcPlugin.getDefault().isBootClass(type.getJavaProject().getProject(), type);
    // setEnabled(!isBootClass);
    // setChecked(isBootClass || getConfigHelper().isBootJarClass(type));
  }

  public void performAction() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IOperationHistory operationHistory = workbench.getOperationSupport().getOperationHistory();
    IUndoContext undoContext = workbench.getOperationSupport().getUndoContext();
    BootJarTypeOperation operation = new BootJarTypeOperation(m_type, isChecked());
    operation.addContext(undoContext);
    try {
      operationHistory.execute(operation, null, null);
    } catch (ExecutionException ee) {
      TcPlugin.getDefault().openError("Executing BootJarTypeOperation", ee);
    }
  }

  class BootJarTypeOperation extends AbstractOperation {
    private final IType   fType;
    private final boolean fMakeBootJarType;

    public BootJarTypeOperation(IType type, boolean makeBootJarType) {
      super("");
      fType = type;
      fMakeBootJarType = makeBootJarType;
      if (fMakeBootJarType) {
        setLabel("Add BootJar Type");
      } else {
        setLabel("Remove BootJar Type");
      }
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeBootJarType) {
        helper.ensureBootJarClass(fType);
      } else {
        helper.ensureNotBootJarClass(fType);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
      ConfigurationHelper helper = getConfigHelper();

      if (fMakeBootJarType) {
        helper.ensureNotBootJarClass(fType);
      } else {
        helper.ensureBootJarClass(fType);
      }
      inspectCompilationUnit();

      return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
      return execute(monitor, info);
    }
  }
}
