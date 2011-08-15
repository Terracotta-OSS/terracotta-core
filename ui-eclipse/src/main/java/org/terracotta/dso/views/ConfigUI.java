/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.ActionUtil;

import com.terracottatech.config.Root;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConfigUI {
  static ISelection convertSelection(ISelection selection) {
    if (selection.isEmpty()) {
      return selection;   
    }
  
    IJavaProject javaProject = ActionUtil.locateSelectedJavaProject(selection);
    IProject project = (javaProject != null) ? javaProject.getProject() : null;

    if (project != null && selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection= (IStructuredSelection) selection;
      List<IJavaElement> javaElements= new ArrayList<IJavaElement>();
      for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
          Object element= iter.next();
          if (element instanceof Root) {
              String rootField= ((Root)element).getFieldName();
              if (rootField != null) {
                TcPlugin plugin = TcPlugin.getDefault();
                ConfigurationHelper configHelper = plugin.getConfigurationHelper(project);
                IField field = configHelper.getField(rootField);
                if(field != null) {
                  javaElements.add(field);
                }
              }
          } else if (element instanceof IMember) {
            javaElements.add((IMember)element);
          }
      }
      return new StructuredSelection(javaElements);
    }
    return StructuredSelection.EMPTY; 
  }

  public static IEditorPart jumpToMember(IJavaElement element) {
    if (element != null) {
      try {
        IEditorPart editor = EditorUtility.openInEditor(element, false);
        JavaUI.revealInEditor(editor, element);
        return editor;
      } catch (Exception e) {
        TcPlugin.getDefault().openError("Java element("+element+")", e);
      }
    }
    return null;
  }

  public static IEditorPart jumpToRegion(IJavaElement element, IRegion region) {
    if (element != null) {
      try {
        IEditorPart editor = EditorUtility.openInEditor(element, false);
        EditorUtility.revealInEditor(editor, region);
        return editor;
      } catch (Exception e) {
        TcPlugin.getDefault().openError("Java element("+element+")", e);
      }
    }
    return null;
  }
}
