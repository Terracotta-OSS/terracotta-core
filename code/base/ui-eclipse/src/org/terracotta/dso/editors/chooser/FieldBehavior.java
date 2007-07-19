/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FieldBehavior implements NavigatorBehavior {

  public static final String  ADD_MSG      = "Enter Fully Qualified Field Name";
  private static final String SELECT_FIELD = "Select Member Field";
  private final List          m_selectedValues;

  public FieldBehavior() {
    this.m_selectedValues = new ArrayList();
  }

  public int style() {
    return SWT.MULTI;
  }

  public String getTitle() {
    return SELECT_FIELD;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IJavaProject) return true;
        if (element instanceof ClassPathContainer || element instanceof ICompilationUnit || element instanceof IType
            || element instanceof IPackageFragmentRoot || element instanceof IClassFile || element instanceof IField) { return true; }
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
        List selectedFields = new ArrayList();
        if (!selection.isEmpty()) {
          Object element;
          for (Iterator i = selection.iterator(); i.hasNext();) {
            if ((element = i.next()) instanceof IField) {
              IField field = (IField) element;
              m_selectedValues.add(resolveFullName(field));
              selectedFields.add(element);
            }
          }
        }
        if (selectedFields.size() > 0) nav.okButtonEnabled(true);
        else nav.okButtonEnabled(false);
      }
    };
  }

  private String resolveFullName(IJavaElement field) {
    List list = new ArrayList();
    list.add(field.getElementName());
    IJavaElement javaPart = field.getParent();
    while (javaPart != null) {
      if (javaPart.getElementType() == IJavaElement.TYPE || javaPart.getElementType() == IJavaElement.PACKAGE_FRAGMENT
          || javaPart.getElementType() == IJavaElement.PACKAGE_DECLARATION) {
        list.add(javaPart.getElementName());

      }
      javaPart = javaPart.getParent();
    }
    String[] parts = (String[]) list.toArray(new String[0]);
    StringBuffer name = new StringBuffer();
    for (int i = parts.length - 1; i >= 1; i--) {
      name.append(parts[i] + ".");
    }
    name.append(parts[0]);
    return name.toString();
  }

  public Object getValues() {
    return m_selectedValues.toArray(new String[0]);
  }
}
