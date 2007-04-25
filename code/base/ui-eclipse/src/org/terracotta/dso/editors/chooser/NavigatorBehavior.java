/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ViewerFilter;

public interface NavigatorBehavior {

  ViewerFilter getFilter(IJavaProject javaProject);

  ISelectionChangedListener getSelectionChangedListener(PackageNavigator navigator);

  Object getValues();
  
  int style();
  
  String getTitle();
}
