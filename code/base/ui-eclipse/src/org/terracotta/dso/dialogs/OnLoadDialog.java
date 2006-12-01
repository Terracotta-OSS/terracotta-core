/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.dijon.Button;
import org.dijon.ButtonGroup;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.Label;
import org.dijon.PagedView;

import org.terracotta.dso.TcPlugin;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTextPane;
import com.terracottatech.configV2.Include;
import com.terracottatech.configV2.OnLoad;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
 * TODO: Merge this with the one in the Configurator. 
 */

public class OnLoadDialog extends Dialog {
  private static DialogResource m_dialogRes;

  private Include     m_include;
  private Label       m_classExprLabel;
  private ButtonGroup m_selectorGroup;
  private PagedView   m_pagedView;
  private XTextField  m_methodNameField;
  private XTextPane   m_codePane;
  
  private static final String NOOP_VIEW    = "NoOp";
  private static final String CALL_VIEW    = "Call";
  private static final String EXECUTE_VIEW = "Execute";
  
  static {
    m_dialogRes = TcPlugin.getDefault().getResources().findDialog("OnLoadDialog"); 
  }

  public OnLoadDialog(Frame frame) {
    super(frame);
    load(m_dialogRes);
    setModal(true);
  }

  public void load(DialogResource dialogRes) {
    super.load(dialogRes);

    m_classExprLabel = (Label)findComponent("ClassExpressionLabel");
    
    m_selectorGroup = (ButtonGroup)findComponent("Selector");
    m_selectorGroup.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_pagedView.setPage(m_selectorGroup.getSelected());
        m_pagedView.revalidate();
        m_pagedView.repaint();
      }
    });
    
    m_pagedView       = (PagedView)findComponent("Views");
    m_methodNameField = (XTextField)findComponent("MethodNameField");
    m_codePane        = (XTextPane)findComponent("CodePane");

    Button closeButton = (Button)findComponent("CloseButton");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String selected = m_selectorGroup.getSelected();
        
        if(selected.equals(NOOP_VIEW)) {
          ensureOnLoadUnset();
        }
        else {
          OnLoad onLoad = ensureOnLoad();
          
          if(selected.equals(CALL_VIEW)) {
            String methodName = m_methodNameField.getText().trim();
            
            if(methodName == null || methodName.length() == 0) {
              ensureOnLoadUnset();
            }
            else {
              if(onLoad.isSetExecute()) {
                onLoad.unsetExecute();
              }
              onLoad.setMethod(methodName);
            }
          }
          else {
            String code = m_codePane.getText().trim();
            
            if(code == null || code.length() == 0) {
              ensureOnLoadUnset();
            }
            else {
              if(onLoad.isSetMethod()) {
                onLoad.unsetMethod();
              }
              onLoad.setExecute(code);
            }
          }
        }
        
        setVisible(false);
      }
    });
    
    Button cancelButton = (Button)findComponent("CancelButton");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
  }
  
  private OnLoad ensureOnLoad() {
    OnLoad onLoad = m_include.getOnLoad();
    return onLoad != null ? onLoad : m_include.addNewOnLoad();
  }
  
  private void ensureOnLoadUnset() {
    if(m_include.isSetOnLoad()) {
      m_include.unsetOnLoad();
    }
  }
  
  public void setInclude(Include include) {
    m_include = include;
    m_classExprLabel.setText(include.getClassExpression());
    
    OnLoad onLoad = include.getOnLoad();
    String view   = NOOP_VIEW;
    
    m_codePane.setText(null);
    m_methodNameField.setText(null);
    
    if(onLoad != null) {
      if(onLoad.isSetExecute()) {
        view = EXECUTE_VIEW;
        m_codePane.setText(onLoad.getExecute());
      }
      else if(onLoad.isSetMethod()) {
        view = CALL_VIEW;
        m_methodNameField.setText(onLoad.getMethod());
      }
    }
    
    m_pagedView.setPage(view);
    m_selectorGroup.setSelected(view);
  }
  
  public Include getInclude() {
    return m_include;
  }
  
  public void edit(Include include) {
    setInclude(include);
    center((java.awt.Component)getOwner());
    setVisible(true);
  }
}
