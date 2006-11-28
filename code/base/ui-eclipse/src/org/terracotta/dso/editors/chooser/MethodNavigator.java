package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;

import org.dijon.DialogResource;

import org.terracotta.dso.editors.tree.JavaProjectModel;

public class MethodNavigator extends JavaProjectNavigator {
  public MethodNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    setTitle("Method Navigator");
  }

  protected JavaProjectModel createModel(IJavaProject javaProject) {
    return new JavaProjectModel(javaProject, false, true, true);
  }
}
