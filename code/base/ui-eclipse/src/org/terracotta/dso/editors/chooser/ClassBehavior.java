/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClassBehavior implements NavigatorBehavior {

  public static final String  ADD_MSG      = "Enter Fully Qualified Class Name";
  private static final String SELECT_CLASS = "Select Class";
  private final List          m_selectedValues;

  public ClassBehavior() {
    this.m_selectedValues = new ArrayList();
  }

  public int style() {
    return SWT.MULTI;
  }

  public String getTitle() {
    return SELECT_CLASS;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IJavaProject && element.equals(javaProject)) return true;
        if (element instanceof ClassPathContainer || element instanceof IPackageFragment
            || element instanceof ICompilationUnit || element instanceof IType
            || element instanceof IPackageFragmentRoot || element instanceof IClassFile) { return true; }
        return false;
      }
    };
  }

  public ISelectionChangedListener getSelectionChangedListener(final PackageNavigator nav) {
    return new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        m_selectedValues.removeAll(m_selectedValues);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        List selectedClasses = new ArrayList();
        if (!selection.isEmpty()) {
          Object element;
          for (Iterator i = selection.iterator(); i.hasNext();) {
            element = i.next();
            if (element instanceof SourceType || element instanceof BinaryType) {
              IType clazz = (IType) element;
              m_selectedValues.add(clazz.getFullyQualifiedName());
              selectedClasses.add(element);
            }
          }
          nav.enableSelection(false, this);
          event.getSelectionProvider().setSelection(new StructuredSelection(selectedClasses.toArray()));
          nav.enableSelection(true, this);
        }
        if (selectedClasses.size() > 0) nav.okButtonEnabled(true);
        else nav.okButtonEnabled(false);
      }
    };
  }

  public Object getValues() {
    return m_selectedValues.toArray(new String[0]);
  }
}
