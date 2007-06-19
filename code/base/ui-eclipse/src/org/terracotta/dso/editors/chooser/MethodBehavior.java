/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.terracotta.dso.PatternHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MethodBehavior implements NavigatorBehavior {

  public static final String  ADD_MSG       = "Enter AspectWerks Method Expression";
  private static final String SELECT_METHOD = "Select Method";
  private final List          m_selectedValues;

  public MethodBehavior() {
    this.m_selectedValues = new ArrayList();
  }

  public int style() {
    return SWT.MULTI;
  }

  public String getTitle() {
    return SELECT_METHOD;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IJavaProject) return true;
        if (element instanceof ClassPathContainer || element instanceof ICompilationUnit || element instanceof IType
            || element instanceof IPackageFragmentRoot || element instanceof IClassFile || element instanceof IMethod) { return true; }
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

  public ISelectionChangedListener getSelectionChangedListener(final PackageNavigator nav) {
    return new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        m_selectedValues.removeAll(m_selectedValues);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        List selectedMethods = new ArrayList();
        if (!selection.isEmpty()) {
          Object element;
          for (Iterator i = selection.iterator(); i.hasNext();) {
            if ((element = i.next()) instanceof IMethod) {
              IMethod method = (IMethod) element;
              m_selectedValues.add(PatternHelper.getExecutionPattern(method));
              selectedMethods.add(element);
            }
          }
          nav.enableSelection(false, this);
          event.getSelectionProvider().setSelection(new StructuredSelection(selectedMethods.toArray()));
          nav.enableSelection(true, this);
        }
        if (selectedMethods.size() > 0) nav.okButtonEnabled(true);
        else nav.okButtonEnabled(false);
      }
    };
  }

  public Object getValues() {
    return m_selectedValues.toArray(new String[0]);
  }
}
