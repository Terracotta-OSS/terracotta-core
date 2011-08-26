/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public interface NavigatorBehavior {

  ILabelProvider getLabelProvider();
  
  ITreeContentProvider getContentProvider();
  
  ViewerFilter getFilter(IJavaProject javaProject);

  ISelectionStatusValidator getValidator();
  
  Object getValues();
  
  int style();
  
  String getMessage();
  
  String getTitle();
}
