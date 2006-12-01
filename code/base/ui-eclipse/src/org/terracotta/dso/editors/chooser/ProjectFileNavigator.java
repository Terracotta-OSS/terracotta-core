/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.dijon.DialogResource;

import org.terracotta.dso.editors.tree.ProjectFileNode;
import org.terracotta.dso.editors.tree.ProjectFileRoot;
import org.terracotta.dso.editors.tree.ProjectModel;

import javax.swing.tree.TreePath;

public class ProjectFileNavigator extends ProjectNavigator {
  private String m_extension;
  
  public ProjectFileNavigator(java.awt.Frame frame) {
    this(frame, null);
  }
  
  public ProjectFileNavigator(java.awt.Frame frame, String extension) {
    super(frame);
    m_extension = extension;
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    setTitle("Project File Navigator");
  }
  
  protected ProjectModel createModel(IProject project) {
    ProjectFileRoot root  = new ProjectFileRoot(project, m_extension);
    ProjectModel    model = new ProjectModel(root);
    
    return model;
  }
  
  protected ProjectFileNode[] getSelectedFileNodes() {
    TreePath[]        paths = m_packageTree.getSelectionPaths();
    ProjectFileNode[] nodes = new ProjectFileNode[paths.length];
    
    for(int i = 0; i < paths.length; i++) {
      nodes[i] = (ProjectFileNode)paths[i].getLastPathComponent();
    }
    
    return nodes;
  }  
  
  public IFile[] getSelectedFiles() {
    ProjectFileNode[] nodes = getSelectedFileNodes();
    IFile[]           result;
    
    if(nodes != null) {
      result = new IFile[nodes.length];
      
      for(int i = 0; i < result.length; i++) {
        result[i] = nodes[i].getFile();
      }
    }
    else {
      result = new IFile[]{};
    }
    
    return result;
  }
  
  public IFile getSelectedFile() {
    IFile[] files = getSelectedFiles();
    return files != null && files.length > 0 ? files[0] : null;
  }
}
