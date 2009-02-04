/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.terracotta.dso.TcPlugin;

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

  public ISelectionStatusValidator getValidator() {
    return new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        m_selectedValue = null;
        for (Object element : selection) {
          if (element instanceof IFile) {
            IFile file = (IFile) element;
            if ("xml".equals(file.getFileExtension())) {
              m_selectedValue = file.getProjectRelativePath().toString();
            }
          }
        }
        String id = TcPlugin.getPluginId();
        if (m_selectedValue == null) { return new Status(IStatus.ERROR, id, IStatus.ERROR, "", null); }
        return new Status(IStatus.OK, id, IStatus.OK, "", null);
      }
    };
  }
}
