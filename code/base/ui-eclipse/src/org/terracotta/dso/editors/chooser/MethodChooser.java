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

public class MethodChooser extends Dialog {
  private static DialogResource  m_res;
  private IProject               m_project;
  private TextField              m_expressionTextField;
  private List                   m_expressionList;
  private DefaultListModel       m_expressionListModel;
  private Button                 m_methodSelectorButton;
  private MethodNavigator        m_methodNavigator;
  private Button                 m_okButton;
  private Button                 m_cancelButton;
  private String[]               m_result;
  private ActionListener         m_listener;

  static {
    TcPlugin           plugin  = TcPlugin.getDefault();
    DictionaryResource topRes  = plugin.getResources();

    m_res = (DialogResource)topRes.find("MethodChooser");
  }
  
  public MethodChooser(java.awt.Frame frame) {
    super(frame);
    if(m_res != null) {
      load(m_res);
    }
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);

    m_expressionTextField = (TextField)findComponent("ExpressionTextField");
    m_expressionTextField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_expressionListModel.addElement(m_expressionTextField.getText());
        m_expressionTextField.selectAll();
      }
    });
    
    m_expressionList = (List)findComponent("ExpressionList");
    m_expressionList.setModel(m_expressionListModel = new DefaultListModel());
    
    m_methodSelectorButton = (Button)findComponent("MethodSelectorButton");
    m_methodSelectorButton.addActionListener(new MethodSelector());
    
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
    m_expressionTextField.setText(null);
    m_expressionListModel.clear();
  }
  
  class MethodSelector implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      if(m_methodNavigator == null) {
        Frame owner = (java.awt.Frame)MethodChooser.this.getOwner();
        
        m_methodNavigator = new MethodNavigator(owner);
        m_methodNavigator.setActionListener(new NavigatorListener());
      }
      m_methodNavigator.init(JavaCore.create(m_project));
      m_methodNavigator.center(MethodChooser.this);
      m_methodNavigator.setModal(true);
      m_methodNavigator.setVisible(true);
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
      String[] sigs = m_methodNavigator.getSelectedSignatures();
      for(int i = 0; i < sigs.length; i++) {
        m_expressionListModel.addElement(sigs[i]);
      }
    }
  }
  
  private void accept() {
    m_result = new String[m_expressionListModel.size()];
    m_expressionListModel.copyInto(m_result);
    setVisible(false);
    fireActionPerformed();
  }
  
  private void cancel() {
    m_result = null;
    setVisible(false);
  }
  public String[] getMethodExpressions() {
    return m_result;
  }
}
