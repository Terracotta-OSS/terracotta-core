/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.tc.admin.common.XRootNode;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents the root of a project fileset
 * 
 * @see JavaProjectNode
 * @see JavaProjectModel
 */

public class ProjectFileRoot extends XRootNode implements IResourceNode {
  private IProject    m_project;
  private String      m_extension;
  private IResource[] m_members;
  
  public ProjectFileRoot(IProject project) {
    this(project, null);
    init();
  }
  
  public ProjectFileRoot(IProject project, String extension) {
    super(project);
    
    m_project   = project;
    m_extension = extension;
    
    init();
  }

  private void init() {
    int count = ensureMembers().length;

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
    IResource[] members = ensureMembers();
    IResource   member;
    ProjectNode child;
    
    for(int i = 0; i < members.length; i++) {
      member = members[i];
      
      switch(member.getType()) {
        case IResource.FOLDER: {
          child = new ProjectFolderNode((IFolder)member, m_extension);
          children.setElementAt(child, i);
          child.setParent(this);
          break;
        }
        case IResource.FILE: {
          child = new ProjectFileNode((IFile)member);
          children.setElementAt(child, i);
          child.setParent(this);
          break;
        }
      }
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
  
  public String getExtension() {
    return m_extension;
  }

  private IResource[] ensureMembers() {
    if(m_members == null) {
      m_members = internalGetMembers();
    }
    return m_members;
  }
  
  private IResource[] internalGetMembers() {
    try {
      IResource[] members = m_project.members();
      IResource   member;
      
      if(m_extension != null) {
        ArrayList list = new ArrayList();
        
        for(int i = 0; i < members.length; i++) {
          member = members[i];
          
          if(member.getType() == IResource.FOLDER ||
             m_extension.equals(member.getFileExtension())) {
            list.add(member);
          }
        }
        members = (IResource[])list.toArray(new IResource[0]);
      }
      
      return members;
    } catch(CoreException ce) {
      return new IResource[0];
    }
  }
}
