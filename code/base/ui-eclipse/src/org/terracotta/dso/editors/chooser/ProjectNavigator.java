/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.terracotta.dso.editors.tree.IResourceNode;
import org.terracotta.dso.editors.tree.ProjectModel;

import javax.swing.tree.TreePath;

public abstract class ProjectNavigator extends BaseProjectNavigator {
  public ProjectNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  protected abstract ProjectModel createModel(IProject project);
  
  public void init(IProject project) {
    m_packageTree.setModel(createModel(project));
  }

  protected IResourceNode[] getSelectedResourceNodes() {
    TreePath[]      paths = m_packageTree.getSelectionPaths();
    IResourceNode[] nodes = new IResourceNode[paths.length];
    
    for(int i = 0; i < paths.length; i++) {
      nodes[i] = (IResourceNode)paths[i].getLastPathComponent();
    }
    
    return nodes;
  }  
  
  public IResource[] getSelectedMembers() {
    IResourceNode[] nodes = getSelectedResourceNodes();
    IResource[]     result;
    
    if(nodes != null) {
      result = new IResource[nodes.length];
      
      for(int i = 0; i < result.length; i++) {
        result[i] = nodes[i].getResource();
      }
    }
    else {
      result = new IResource[]{};
    }
    
    return result;
  }
  
  public IResource getSelectedMember() {
    IResourceNode[] nodes = getSelectedResourceNodes();
    return nodes != null && nodes.length > 0 ? nodes[0].getResource() : null;
  }
}
