/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.tc.admin.common.XRootNode;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a project root.
 * 
 * @see ProjectNode
 * @see ProjectModel
 * @see IResourceNode
 */

public class ProjectFolderRoot extends XRootNode implements IResourceNode {
  private IProject  m_project;
  private IFolder[] m_folders;
  
  public ProjectFolderRoot(IProject project) {
    super(project);
    m_project = project;
    init();
  }

  private void init() {
    int count = ensureFolders().length;

    if(children == null) {
      children = new Vector();
    }
    children.setSize(count);
  }
  
  public TreeNode getChildAt(int index) {
    if(children != null && children.elementAt(index) == null) {
      addChildren();
    }
    
    return super.getChildAt(index);
  }
  
  private void addChildren() {
    IFolder[]  folders = ensureFolders();
    FolderNode child;
    
    for(int i = 0; i < folders.length; i++) {
      child = new FolderNode(folders[i]);
      children.setElementAt(child, i);
      child.setParent(this);
    }
  }
  
  class FolderNode extends ProjectFolderNode {
    FolderNode(IFolder folder) {
      super(folder);
    }
    
    FolderNode(IFolder folder, String extension) {
      super(folder, extension);
    }

    protected ProjectFolderNode createFolderNode(IFolder folder, String ext) {
      return new FolderNode(folder, ext);
    }
    
    public boolean addResource(IResource resource) {
      return resource instanceof IFolder;
    }
  }
  
  public String toString() {
    return m_project.getName();
  }
  
  public IResource getResource() {
    return getProject();
  }
  
  public IProject getProject() {
    return m_project;
  }
  
  private IFolder[] ensureFolders() {
    if(m_folders == null) {
      m_folders = internalGetFolders();
    }
    return m_folders;
  }
  
  private IFolder[] internalGetFolders() {
    try {
      ArrayList<IFolder> list = new ArrayList<IFolder>();
      IResource[] members = m_project.members();
          
      for(int i = 0; i < members.length; i++) {
        if(members[i].getType() == IResource.FOLDER) {
          list.add((IFolder)members[i]);
        }
      }

      return list.toArray(new IFolder[0]);
    } catch(CoreException ce) {
      return new IFolder[0];
    }
  }
}
