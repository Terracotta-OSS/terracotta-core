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

public class FieldChooser extends Dialog {
  private static DialogResource m_res;
  private IProject              m_project;
  private TextField             m_fieldTextField;
  private List                  m_fieldList;
  private DefaultListModel      m_fieldListModel;
  private Button                m_fieldSelectorButton;
  private FieldNavigator        m_fieldNavigator;
  private Button                m_okButton;
  private Button                m_cancelButton;
  private String[]              m_result;
  private ActionListener        m_listener;

  static {
    TcPlugin           plugin  = TcPlugin.getDefault();
    DictionaryResource topRes  = plugin.getResources();

    m_res = (DialogResource)topRes.find("FieldChooser");
  }
  
  public FieldChooser(Frame frame) {
    super(frame);
    if(m_res != null) {
      load(m_res);
    }
  }
  
  public void load(DialogResource dialogRes) {
    super.load(dialogRes);

    m_fieldTextField = (TextField)findComponent("FieldTextField");
    m_fieldTextField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_fieldListModel.addElement(m_fieldTextField.getText());
        m_fieldTextField.selectAll();
      }
    });
    
    m_fieldList = (List)findComponent("FieldList");
    m_fieldList.setModel(m_fieldListModel = new DefaultListModel());

    m_fieldSelectorButton = (Button)findComponent("FieldSelectorButton");
    m_fieldSelectorButton.addActionListener(new FieldSelector());
    
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
    m_fieldTextField.setText(null);
    m_fieldListModel.clear();
  }
  
  class FieldSelector implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      if(m_fieldNavigator == null) {
        m_fieldNavigator = new FieldNavigator((Frame)FieldChooser.this.getOwner());
        m_fieldNavigator.setActionListener(new NavigatorListener());
      }
      m_fieldNavigator.init(JavaCore.create(m_project));
      m_fieldNavigator.center(FieldChooser.this);
      m_fieldNavigator.setModal(true);
      m_fieldNavigator.setVisible(true);
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
      String[] fields = m_fieldNavigator.getSelectedFieldNames();
      for(int i = 0; i < fields.length; i++) {
        m_fieldListModel.addElement(fields[i]);
      }
    }
  }
  
  private void accept() {
    m_fieldListModel.copyInto(m_result = new String[m_fieldListModel.size()]);
    setVisible(false);
    fireActionPerformed();
  }
  
  private void cancel() {
    m_result = null;
    setVisible(false);
  }
  
  public String[] getFieldNames() {
    return m_result;
  }
}
