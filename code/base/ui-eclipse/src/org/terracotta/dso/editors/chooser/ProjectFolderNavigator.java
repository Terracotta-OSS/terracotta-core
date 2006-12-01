/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.dijon.DialogResource;

import org.terracotta.dso.editors.tree.ProjectFolderNode;
import org.terracotta.dso.editors.tree.ProjectFolderRoot;
import org.terracotta.dso.editors.tree.ProjectModel;

import javax.swing.tree.TreePath;

public class ProjectFolderNavigator extends ProjectNavigator {
  public ProjectFolderNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    setTitle("Project Directory Navigator");
  }
  
  protected ProjectModel createModel(IProject project) {
    return new ProjectModel(new ProjectFolderRoot(project));
  }
  
  protected ProjectFolderNode[] getSelectedFolderNodes() {
    TreePath[]          paths = m_packageTree.getSelectionPaths();
    ProjectFolderNode[] nodes = new ProjectFolderNode[paths.length];
    
    for(int i = 0; i < paths.length; i++) {
      nodes[i] = (ProjectFolderNode)paths[i].getLastPathComponent();
    }
    
    return nodes;
  }  
  
  public IFolder[] getSelectedFolders() {
    ProjectFolderNode[] nodes = getSelectedFolderNodes();
    IFolder[]           result;
    
    if(nodes != null) {
      result = new IFolder[nodes.length];
      
      for(int i = 0; i < result.length; i++) {
        result[i] = nodes[i].getFolder();
      }
    }
    else {
      result = new IFolder[]{};
    }
    
    return result;
  }
  
  public IFolder getSelectedFolder() {
    IFolder[] folders = getSelectedFolders();
    return folders != null && folders.length > 0 ? folders[0] : null;
  }
}
