/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.internal.resources.Folder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;

public final class FolderBehavior implements NavigatorBehavior {

  private static final String SELECT_FOLDER = "Select Folder";
  private String              m_selectedValue;

  public int style() {
    return SWT.SINGLE;
  }

  public String getTitle() {
    return SELECT_FOLDER;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IJavaProject && element.equals(javaProject)) return true;
        if (element instanceof Folder) return true;
        return false;
      }
    };
  }

  public ISelectionChangedListener getSelectionChangedListener(final PackageNavigator nav) {
    return new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        nav.okButtonEnabled(true);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        if (!selection.isEmpty()) {
          Object element = selection.getFirstElement();
          if (element != null) {
            if (element instanceof IJavaProject) {
              nav.enableSelection(false, this);
              event.getSelectionProvider().setSelection(null);
              nav.enableSelection(true, this);
              nav.okButtonEnabled(false);
            } else if (element instanceof Folder) {
              Folder folder = (Folder) element;
              m_selectedValue = folder.getProjectRelativePath().toString();
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
