/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlObject;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XComboBox;
import org.terracotta.dso.editors.ConfigurationEditorPanel;

import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;

public class XmlStringEnumCombo extends XComboBox
  implements XmlObjectHolder
{
  private XmlObjectHolderHelper m_helper;
  private boolean               m_listening;
  
  public XmlStringEnumCombo() {
    super();
    m_helper = new XmlObjectHolderHelper();
    getActionMap().put(RESET, new ResetAction());
    getInputMap().put(RESET_STROKE, RESET);
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
    setModel(new DefaultComboBoxModel(m_helper.getEnumValues()));
  }

  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setSelectedItem(enumValue());
    if(isSet()) {
      m_helper.validateXmlObject(this);
    }
    m_listening = true;
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
  }
  
  protected void fireActionEvent() {
    if(m_listening) {
      set();
    }
    super.fireActionEvent();
  }
  
  public String getSelectedEnumString() {
    return getSelectedItem().toString();
  }
  
  public int getSelectedEnumInt() {
    return ((StringEnumAbstractBase)getSelectedItem()).intValue();
  }
  
  public StringEnumAbstractBase enumValue() {
    return isSet() ? m_helper.getEnumValue() : m_helper.defaultEnumValue();
  }
  
  public boolean isRequired() {
    return m_helper.isRequired();
  }
  
  public boolean isSet() {
    return m_helper.isSet();
  }
  
  public void set() {
    Object item = getSelectedItem();
    String s    = item.toString();
    
    ensureXmlObject();
    m_helper.set(s);
    setSelectedItem(item);
    m_helper.validateXmlObject(this);
  }
  
  public void unset() {
    if(!isRequired()) {
      m_listening = false;
      m_helper.unset();    
      setSelectedItem(m_helper.defaultEnumValue());
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
    }
    
    public void actionPerformed(ActionEvent ae) {
      unset();
    }
  }
}
