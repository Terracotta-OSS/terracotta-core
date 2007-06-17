/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.editors.ConfigurationEditorPanel;
import org.terracotta.ui.util.SWTUtil;

public class XmlStringField implements XmlObjectHolder {
  private XmlObjectHolderHelper m_helper;
  private Text                  m_field;
  private boolean               m_listening;

  public XmlStringField(Text field) {
    m_helper = new XmlObjectHolderHelper();
    m_field = field;
    field.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        if(m_listening) {
          set();
        }
      }
    });
    field.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(!m_listening) return;
        switch(e.keyCode) {
          case SWT.Selection: {
            set();
            break;
          }
          case SWT.F5: {
            unset();
            break;
          }
        }
      }
    });
  }

  protected void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      SWTUtil.getAncestorOfClass(ConfigurationEditorPanel.class, m_field);
    
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }
  
  public void init(Class parentType, String elementName) {
    m_helper.init(parentType, elementName);
  }
  
  public void setup(XmlObject parent) {
    setListening(false);
    m_helper.setup(parent);
    setText(stringValue());
    setListening(true);
  }
  
  public void tearDown() {
    m_helper.tearDown();
    setListening(false);
    setText("");
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
    setListening(false);
    try {
      String s = getText();
      if(m_helper.hasDefault() && m_helper.defaultStringValue().equals(s)) {
        unset();
      } else if(!s.equals(m_helper.getStringValue())){
        ensureXmlObject();
        m_helper.set(s);
        m_field.setText(s);
      }
    } finally {
      setListening(true);
    }
  }
  
  public void unset() {
    if(!isRequired()) {
      setListening(false);
      try {
        m_helper.unset();
        setText(m_helper.defaultStringValue());
      } finally {
        setListening(true);
      }
    }
  }
  
  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }
  
  public String getText() {
    return m_field.getText();
  }
  
  public void setText(String text) {
    m_field.setText(text != null ? text : "");
  }
  
  private void setListening(boolean listening) {
    if(m_listening != listening) {
      m_listening = listening;
    }
  }
}

