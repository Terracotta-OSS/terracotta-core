/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

public class ConfigFileBehavior extends FileBehavior {
  private static final String SELECT_CONFIG_FILE_LOCATION = "Select Config File Location";

  public String getTitle() {
    return SELECT_CONFIG_FILE_LOCATION;
  }

  protected boolean filterSelect(Viewer viewer, Object parentElement, Object element) {
    boolean result = super.filterSelect(viewer, parentElement, element);
    if (result && element instanceof IFile) {
      result = "xml".equals(((IFile) element).getFileExtension());
    }
    return result;
  }

  public ISelectionChangedListener getSelectionChangedListener(final PackageNavigator nav) {
    return new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        m_selectedValue = null;
        nav.okButtonEnabled(true);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        if (!selection.isEmpty()) {
          Object element = selection.getFirstElement();
          if (element instanceof IFile) {
            IFile file = (IFile) element;
            if ("xml".equals(file.getFileExtension())) {
              m_selectedValue = file.getProjectRelativePath().toOSString();
            }
          }
        }
        nav.okButtonEnabled(m_selectedValue != null);
      }
    };
  }
}
