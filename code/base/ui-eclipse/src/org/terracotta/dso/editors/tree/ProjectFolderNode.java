/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.terracotta.dso.TcPlugin;
import com.tc.admin.common.XAbstractAction;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeNode;

/**
 * A TreeNode that represents a project folder.
 * 
 * @see ProjectNode
 * @see IResourceNode
 */

public class ProjectFolderNode extends ProjectNode implements IResourceNode {
  private IFolder     m_folder;
  private String      m_extension;
  private IResource[] m_members;
  private JPopupMenu  m_popup;
  
  public ProjectFolderNode(IFolder folder) {
    this(folder, null);
    initPopup();
    init();
  }

  private void initPopup() {
    m_popup = new JPopupMenu();
    m_popup.add(new NewFolderAction());
  }

  public JPopupMenu getPopupMenu() {
    return m_popup;
  }
  
  public ProjectFolderNode(IFolder folder, String extension) {
    super(folder);
    
    m_folder    = folder;
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

  protected void removeChildren() {
    removeAllChildren();
    nodeStructureChanged();
  }
  
  protected void addChildren() {
    IResource[] members = ensureMembers();
    IResource   member;

    for(int i = 0; i < members.length; i++) {
      member = members[i];
      
      switch(member.getType()) {
        case IResource.FOLDER: {
          setChildAt(createFolderNode((IFolder)member, m_extension), i);
          break;
        }
        case IResource.FILE: {
          setChildAt(new ProjectFileNode((IFile)member), i);
          break;
        }
      } 
    }
  }
  
  protected ProjectFolderNode createFolderNode(IFolder folder, String extension) {
    return new ProjectFolderNode(folder, extension);
  }
  
  protected boolean addResource(IResource resource) {
    return resource.getType() == IResource.FOLDER ||
           m_extension == null ||
           m_extension.equals(resource.getFileExtension());
  }
  
  protected void refresh() {
    removeChildren();
    addChildren();
  }
  
  public String toString() {
    return m_folder.getName();
  }

  public IResource getResource() {
    return getFolder();
  }
  
  public IFolder getFolder() {
    return m_folder;
  }
  
  public String getExtension() {
    return m_extension;
  }
  
  class NewFolderAction extends XAbstractAction {
    NewFolderAction() {
      super("Create folder...");
    }
    
    public void actionPerformed(ActionEvent ae) {
      Object               src = ae.getSource();
      KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      Component            pfo = kfm.getPermanentFocusOwner();

      System.out.println("src: " + src);
      
      String name = JOptionPane.showInputDialog(pfo, "Enter new folder name");
      
      try {
        m_folder.getFolder(name).create(true, true, null);
        refresh();
      } catch(CoreException ce) {
        TcPlugin.getDefault().openError("Unable to create folder", ce);
      }
    }
  }
  
  private IResource[] ensureMembers() {
    if(m_members == null) {
      m_members = internalGetMembers();
    }
    return m_members;
  }
  
  private IResource[] internalGetMembers() {
    try {
      ArrayList   list    = new ArrayList();
      IResource[] members = m_folder.members();

      for(int i = 0; i < members.length; i++) {
        if(addResource(members[i])) {
          list.add(members[i]);
        }
      }
      
      return (IResource[])list.toArray(new IResource[0]);
    } catch(CoreException ce) {
      return new IResource[0];
    }
  }
}
