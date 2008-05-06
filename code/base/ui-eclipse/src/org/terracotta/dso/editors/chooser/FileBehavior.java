/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;

public class FileBehavior implements NavigatorBehavior {

  private static final String SELECT_FILE = "Select File";
  protected String            m_selectedValue;

  public int style() {
    return SWT.SINGLE;
  }

  public String getTitle() {
    return SELECT_FILE;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        return filterSelect(viewer, parentElement, element);
      }
    };
  }

  protected boolean packageFragmentContainsJavaResources(final IPackageFragment packageFragment) {
    try {
      return packageFragment.containsJavaResources();
    } catch (JavaModelException jme) {
      return false;
    }
  }

  protected boolean isPackageFragmentBinaryKind(final IPackageFragment packageFragment) {
    try {
      return packageFragment.getKind() == IPackageFragmentRoot.K_BINARY;
    } catch (JavaModelException jme) {
      return false;
    }
  }

  protected boolean filterSelect(Viewer viewer, Object parentElement, Object element) {
    if (element instanceof IJavaModel) return true;
    if (element instanceof IJavaProject) return true;
    if (element instanceof IFolder) return true;
    if (element instanceof IFile) return true;
    if (element instanceof IPackageFragment) {
      IPackageFragment packageFragment = (IPackageFragment) element;
      if (isPackageFragmentBinaryKind(packageFragment)
          || (packageFragment.isDefaultPackage() && !packageFragmentContainsJavaResources(packageFragment))) { return false; }
      return true;
    }
    if (element instanceof IPackageFragmentRoot) return true;
    if (element instanceof ClassPathContainer) {
      ClassPathContainer container = (ClassPathContainer) element;
      IClasspathEntry cpe = container.getClasspathEntry();
      return (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE);
    }
    if (element instanceof ICompilationUnit) return true;
    return false;
  }

  public ISelectionChangedListener getSelectionChangedListener(final PackageNavigator nav) {
    return new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        nav.okButtonEnabled(true);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        if (!selection.isEmpty()) {
          Object element = selection.getFirstElement();
          if (element != null) {
            if (element instanceof IJavaProject || element instanceof IFolder) {
              nav.okButtonEnabled(false);
            } else if (element instanceof IFile) {
              IFile file = (IFile) element;
              m_selectedValue = file.getProjectRelativePath().toString();
              nav.okButtonEnabled(true);
            }
          }
        }
      }
    };
  }

  public Object getValues() {
    return m_selectedValue;
  }
}
