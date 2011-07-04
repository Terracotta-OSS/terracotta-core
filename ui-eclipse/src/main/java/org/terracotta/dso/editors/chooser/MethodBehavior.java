/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.terracotta.dso.PatternHelper;
import org.terracotta.dso.TcPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MethodBehavior extends AbstractNavigatorBehavior {

  public static final String  ADD_MSG       = "Enter Method Expression";
  private static final String SELECT_METHOD = "Select Method";
  private final List          m_selectedValues;

  public MethodBehavior() {
    this.m_selectedValues = new ArrayList();
  }

  @Override
  public int style() {
    return SWT.MULTI;
  }

  @Override
  public String getTitle() {
    return SELECT_METHOD;
  }

  @Override
  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IJavaProject) return true;
        if (parentElement instanceof IJavaProject) {
          if (element instanceof IFile || element instanceof IFolder) return false;
          if (element instanceof IPackageFragment) {
            try {
              return ((IPackageFragment) element).containsJavaResources();
            } catch (JavaModelException jme) {/**/
            }
          }
          return true;
        }
        if (element instanceof ICompilationUnit || element instanceof IType || element instanceof IClassFile
            || element instanceof IMethod) { return true; }
        if (element instanceof IPackageFragment) {
          try {
            return ((IPackageFragment) element).containsJavaResources();
          } catch (JavaModelException jme) {/**/
          }
        }
        return false;
      }
    };
  }

  @Override
  public ISelectionStatusValidator getValidator() {
    return new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        m_selectedValues.clear();
        for (Object element : selection) {
          if (element instanceof IMethod) {
            IMethod method = (IMethod) element;
            m_selectedValues.add(PatternHelper.getExecutionPattern(method));
          }
        }
        String id = TcPlugin.getPluginId();
        if (m_selectedValues.size() == 0) { return new Status(IStatus.ERROR, id, IStatus.ERROR, "", null); }
        return new Status(IStatus.OK, id, IStatus.OK, "", null);
      }
    };
  }

  @Override
  public Object getValues() {
    return m_selectedValues.toArray(new String[0]);
  }

  @Override
  public String getMessage() {
    return ADD_MSG;
  }
}
