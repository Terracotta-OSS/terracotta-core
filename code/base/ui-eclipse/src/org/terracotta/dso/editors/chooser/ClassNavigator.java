package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;

import org.dijon.DialogResource;

import org.terracotta.dso.editors.tree.JavaProjectModel;

public class ClassNavigator extends JavaProjectNavigator {
  public ClassNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    setTitle("Class Navigator");
  }
  
  public JavaProjectModel createModel(IJavaProject javaProject) {
    return new JavaProjectModel(javaProject, false, false, true);
  }
}
