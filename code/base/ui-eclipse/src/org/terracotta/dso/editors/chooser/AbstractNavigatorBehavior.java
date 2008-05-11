/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public abstract class AbstractNavigatorBehavior implements NavigatorBehavior {

  public ITreeContentProvider getContentProvider() {
    PackageExplorerContentProvider contentProvider = new PackageExplorerContentProvider(true);
    contentProvider.setIsFlatLayout(true);
    return contentProvider;
  }

  public ILabelProvider getLabelProvider() {
    ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
    return labelProvider;
  }

  public ViewerFilter getFilter(IJavaProject javaProject) {
    return null;
  }

  public ISelectionStatusValidator getValidator() {
    return null;
  }

  public abstract String getMessage();

  public abstract String getTitle();

  public abstract Object getValues();

  public abstract int style();

}
