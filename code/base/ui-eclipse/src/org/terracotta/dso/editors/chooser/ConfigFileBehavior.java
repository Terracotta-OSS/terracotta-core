/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

public class ConfigFileBehavior extends FileBehavior {
  private static final String SELECT_CONFIG_FILE_LOCATION = "Select Config File Location";
  private static final String DEFAULT_CONFIG_FILE_NAME = "tc-config.xml";
  
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
        nav.okButtonEnabled(true);
        StructuredSelection selection = (StructuredSelection) event.getSelection();
        if (!selection.isEmpty()) {
          Object element = selection.getFirstElement();
          if (element != null) {
            if (element instanceof IJavaProject) {
              m_selectedValue = DEFAULT_CONFIG_FILE_NAME;
            } else if(element instanceof IFolder) {
              IFolder folder = (IFolder) element;
              m_selectedValue = folder.getProjectRelativePath().append(DEFAULT_CONFIG_FILE_NAME).toOSString();
            } else if (element instanceof IFile) {
              IFile file = (IFile) element;
              m_selectedValue = file.getProjectRelativePath().toOSString();
            }
          }
        }
      }
    };
  }
}
