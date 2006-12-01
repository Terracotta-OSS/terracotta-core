/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;

import org.dijon.Button;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.DictionaryResource;
import org.dijon.List;
import org.dijon.TextField;

import org.terracotta.dso.TcPlugin;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;

/**
 * TODO: Factor out JavaProjectChooser from TransientChooser, RootChooser, and LockChooser.
 */

public class ClassChooser extends Dialog {
  private static DialogResource m_res;
  private IProject              m_project;
  private TextField             m_classTextField;
  private List                  m_classList;
  private DefaultListModel      m_classListModel;
  private Button                m_classSelectorButton;
  private ClassNavigator        m_classNavigator;
  private Button                m_okButton;
  private Button                m_cancelButton;
  private String[]              m_result;
  private ActionListener        m_listener;

  static {
    TcPlugin           plugin  = TcPlugin.getDefault();
    DictionaryResource topRes  = plugin.getResources();

    m_res = (DialogResource)topRes.find("ClassChooser");
  }
  
  public ClassChooser(java.awt.Frame frame) {
    super(frame);
    if(m_res != null) {
      load(m_res);
    }
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);

    m_classTextField = (TextField)findComponent("ClassTextField");
    m_classTextField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_classListModel.addElement(m_classTextField.getText());
        m_classTextField.selectAll();
      }
    });
    
    m_classList = (List)findComponent("ClassList");
    m_classList.setModel(m_classListModel = new DefaultListModel());
    
    m_classSelectorButton = (Button)findComponent("ClassSelectorButton");
    m_classSelectorButton.addActionListener(new ClassSelector());
    
    m_okButton = (Button)findComponent("OKButton");
    m_okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        accept();
      }
    });
    m_cancelButton = (Button)findComponent("CancelButton");
    m_cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        cancel();
      }
    });
    
    getRootPane().setDefaultButton(m_okButton);
  }
  
  public void setup(IProject project) {
    m_project = project;
    m_classTextField.setText(null);
    m_classListModel.clear();
  }
  
  class ClassSelector implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      if(m_classNavigator == null) {
        Frame owner = (java.awt.Frame)ClassChooser.this.getOwner();
        
        m_classNavigator = new ClassNavigator(owner);
        m_classNavigator.setActionListener(new NavigatorListener());
      }
      m_classNavigator.init(JavaCore.create(m_project));
      m_classNavigator.center(ClassChooser.this);
      m_classNavigator.setModal(true);
      m_classNavigator.setVisible(true);
    }
  }

  public void setListener(ActionListener listener) {
    m_listener = listener;
  }
  
  protected void fireActionPerformed() {
    m_listener.actionPerformed(null);
  }
  
  class NavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] sigs = m_classNavigator.getSelectedSignatures();
      for(int i = 0; i < sigs.length; i++) {
        m_classListModel.addElement(sigs[i]);
      }
    }
  }
  
  private void accept() {
    m_result = new String[m_classListModel.size()];
    m_classListModel.copyInto(m_result);
    setVisible(false);
    fireActionPerformed();
  }
  
  private void cancel() {
    m_result = null;
    setVisible(false);
  }
  
  public String[] getClassnames() {
    return m_result;
  }
}
