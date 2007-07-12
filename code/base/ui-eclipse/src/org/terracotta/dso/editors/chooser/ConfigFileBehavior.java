/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.Viewer;

public class ConfigFileBehavior extends FileBehavior {
  protected boolean filterSelect(Viewer viewer, Object parentElement, Object element) {
    boolean result = super.filterSelect(viewer, parentElement, element);
    if (result && element instanceof IFile) {
      result = "xml".equals(((IFile) element).getFileExtension());
    }
    return result;
  }
}
