/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import org.dijon.Separator;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTextField;
import org.terracotta.dso.editors.ConfigurationEditorPanel;

import java.awt.event.ActionEvent;

import javax.swing.JPopupMenu;

public class XmlStringField extends XTextField
  implements XmlObjectHolder
{
  private XmlObjectHolderHelper m_helper;
  private boolean               m_listening;

  public XmlStringField() {
    super();
    m_helper = new XmlObjectHolderHelper();
    getActionMap().put(RESET, new ResetAction());
    getInputMap().put(RESET_STROKE, RESET);
  }

  protected JPopupMenu createPopup() {
    JPopupMenu popup = super.createPopup();
    
    if(popup == null) {
      popup = new JPopupMenu();
    }
    else {
      popup.add(new Separator());
    }
    
    popup.add(new ResetAction());
    
    return popup;
  }
  
  protected void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      getAncestorOfClass(ConfigurationEditorPanel.class, this);
    
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }
  
  public void init(Class parentType, String elementName) {
    m_helper.init(parentType, elementName);
  }
  
  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setText(stringValue());
    if(isSet()) {
      m_helper.validateXmlObject(this);
    }
    m_listening = true;
  }
  
  protected void fireActionPerformed() {
    if(m_listening) {
      set();
    }
    super.fireActionPerformed();
  }
  
  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    setText(null);
  }
  
  public String stringValue() {
    return isSet() ? m_helper.getStringValue() : m_helper.defaultStringValue();
  }

  public boolean isRequired() {
    return m_helper.isRequired();
  }
  
  public boolean isSet() {
    return m_helper.isSet();
  }

  public void set() {
    String s = getText();
    
    ensureXmlObject();
    m_helper.set(s);
    setText(s);
    m_helper.validateXmlObject(this);
  }
  
  public void unset() {
    if(!isRequired()) {
      m_listening = false;
      m_helper.unset();
      setText(m_helper.defaultStringValue());
      m_listening = true;
    }
  }
  
  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }

  class ResetAction extends XAbstractAction {
    ResetAction() {
      super("Reset");
      setShortDescription("Reset to default value");
      setAccelerator(RESET_STROKE);
    }
    
    public void actionPerformed(ActionEvent ae) {
      unset();
    }
  }
}

