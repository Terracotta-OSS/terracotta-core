/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
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

  public static void jumpToMember(IJavaElement element) {
    if (element != null) {
      try {
        IEditorPart editor = EditorUtility.openInEditor(element, false);
        JavaUI.revealInEditor(editor, element);
      } catch (JavaModelException e) {
        JavaPlugin.log(e);
      } catch (PartInitException e) {
        JavaPlugin.log(e);
      }
    }
  }
}
