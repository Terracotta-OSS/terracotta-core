/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;

import org.terracotta.dso.editors.tree.JavaProjectModel;
import org.terracotta.dso.editors.tree.JavaProjectNode;
import org.terracotta.dso.editors.tree.ProjectNode;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class JavaProjectNavigator extends BaseProjectNavigator {
  protected String[] m_signatures;
  
  public JavaProjectNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  protected abstract JavaProjectModel createModel(IJavaProject javaProject);
  
  public void init(IJavaProject javaProject) {
    m_packageTree.setModel(createModel(javaProject));
    m_signatures = null;
  }

  public String[] getSelectedSignatures() {
    ProjectNode[] nodes = getSelection();
    
    m_signatures = new String[nodes.length];
    
    for(int i = 0; i < nodes.length; i++) {
      m_signatures[i] = ((JavaProjectNode)nodes[i]).getSignature();
    }
    
    return m_signatures;
  }

  public String[] getSelectedFields() {
    ProjectNode[] nodes = getSelection();
    ArrayList     list  = new ArrayList();

    for(int i = 0; i < nodes.length; i++) {
      list.addAll(Arrays.asList(((JavaProjectNode)nodes[i]).getFields()));
    }
    
    return (String[])list.toArray(new String[0]);
  }
}
