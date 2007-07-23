/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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

  protected boolean filterSelect(Viewer viewer, Object parentElement, Object element) {
    if (element instanceof IJavaModel) return true;
    if (element instanceof IJavaProject) return true;
    if (element instanceof IFolder) return true;
    if (element instanceof IFile) return true;
    if (element instanceof IPackageFragment) return true;
    if (element instanceof IPackageFragmentRoot) return true;
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
