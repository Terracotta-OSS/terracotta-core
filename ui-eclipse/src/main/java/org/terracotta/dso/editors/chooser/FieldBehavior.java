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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.terracotta.dso.TcPlugin;

import java.util.ArrayList;
import java.util.List;

public final class FieldBehavior extends AbstractNavigatorBehavior {

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
            || element instanceof IField) { return true; }
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

  public ISelectionStatusValidator getValidator() {
    return new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        m_selectedValues.clear();
        for (Object element : selection) {
          if (element instanceof IField) {
            IField field = (IField) element;
            m_selectedValues.add(resolveFullName(field));
          }
        }
        String id = TcPlugin.getPluginId();
        if (m_selectedValues.size() == 0) {
          return new Status(IStatus.ERROR, id, IStatus.ERROR, "", null);
        }
        return new Status(IStatus.OK, id, IStatus.OK, "", null);
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

  @Override
  public String getMessage() {
    return ADD_MSG;
  }
}
