/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.jdt.core.IJavaProject;

import org.dijon.DialogResource;

import com.tc.admin.common.XTreeNode;
import org.terracotta.dso.editors.tree.FieldNode;
import org.terracotta.dso.editors.tree.JavaProjectModel;
import org.terracotta.dso.editors.tree.TypeNode;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class FieldNavigator extends JavaProjectNavigator {
  public FieldNavigator(java.awt.Frame frame) {
    super(frame);
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);
    setTitle("Field Navigator");
    
    m_packageTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        TreePath newPath  = e.getNewLeadSelectionPath();
        
        if(newPath == null) {
          return;
        }
        
        Object lastNode = newPath.getLastPathComponent();
        
        if(lastNode instanceof TypeNode) {
          TypeNode  typeNode   = (TypeNode)lastNode;
          int       childCount = typeNode.getChildCount();
          XTreeNode childNode;
          
          for(int i = 0; i < childCount; i++) {
            childNode = (XTreeNode)typeNode.getChildAt(i);
            
            if(childNode instanceof FieldNode) {
              TreePath childPath = new TreePath(childNode.getPath());
              
              if(m_packageTree.isPathSelected(childPath)) {
                m_packageTree.removeSelectionPath(childPath);
              }
            }
          }
        }
        else if(lastNode instanceof FieldNode) {
          FieldNode fieldNode = (FieldNode)lastNode;
          TypeNode  typeNode  = (TypeNode)fieldNode.getParent();
          
          if(typeNode != null) {
            TreePath  typePath  = new TreePath(typeNode.getPath());
            
            if(m_packageTree.isPathSelected(typePath)) {
              m_packageTree.removeSelectionPath(typePath);
            }
          }
        }
      }
    });
  }

  public JavaProjectModel createModel(IJavaProject javaProject) {
    return new JavaProjectModel(javaProject, true, false, true);
  }

  public String[] getSelectedFieldNames() {
    return getSelectedFields();
  }
}
