/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.dijon.Button;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.DictionaryResource;
import org.dijon.Tree;

import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.tree.ProjectNode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public abstract class BaseProjectNavigator extends Dialog {
  private static DialogResource m_res;
  protected Tree                m_packageTree;
  protected Button              m_okButton;
  protected Button              m_cancelButton;
  protected ProjectNode[]       m_selection;
  private   ActionListener      m_listener;

  static {
    TcPlugin           plugin  = TcPlugin.getDefault();
    DictionaryResource topRes  = plugin.getResources();

    m_res = (DialogResource)topRes.find("JavaProjectNavigator");
  }
  
  public BaseProjectNavigator(java.awt.Frame frame) {
    super(frame);
    if(m_res != null) {
      load(m_res);
    }
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);

    m_packageTree = (Tree)findComponent("PackageTree");
    m_packageTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        TreeNode node = (TreeNode)m_packageTree.getLastSelectedPathComponent();

        if(node.getChildCount() == 0 && me.getClickCount() == 2) {
          m_okButton.doClick();
        }
      }
    });
    
    m_okButton = (Button)findComponent("OKButton");
    m_okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_selection = getSelectedProjectNodes();
        setVisible(false);
        fireActionPerformed();
      }
    });
    
    m_cancelButton = (Button)findComponent("CancelButton");
    m_cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_selection = null;
        setVisible(false);
      }
    });
    
    getRootPane().setDefaultButton(m_okButton);
  }

  protected ProjectNode[] getSelectedProjectNodes() {
    TreePath[]    paths = m_packageTree.getSelectionPaths();
    ProjectNode[] nodes = new ProjectNode[paths.length];
    
    for(int i = 0; i < paths.length; i++) {
      nodes[i] = (ProjectNode)paths[i].getLastPathComponent();
    }
    
    return nodes;
  }
  
  public void setActionListener(ActionListener listener) {
    m_listener = listener;
  }
  
  protected void fireActionPerformed() {
    m_listener.actionPerformed(null);
  }
  
  public ProjectNode[] getSelection() {
    return m_selection;
  }
}
