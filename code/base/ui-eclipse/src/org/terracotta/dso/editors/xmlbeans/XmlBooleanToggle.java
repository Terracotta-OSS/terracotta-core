/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.terracotta.dso.editors.ConfigurationEditorPanel;
import org.terracotta.ui.util.SWTUtil;

public class XmlBooleanToggle implements XmlObjectHolder {
  private final XmlObjectHolderHelper m_helper;
  private final Button                m_button;
  private final SelectionListener     m_selectionListener;
  private boolean                     m_listening;

  public static XmlBooleanToggle newInstance(Button button) {
    XmlBooleanToggle toggle = new XmlBooleanToggle(button);
    toggle.addListeners();
    return toggle;
  }

  protected XmlBooleanToggle(Button button) {
    m_helper = new XmlObjectHolderHelper();
    m_button = button;
    m_selectionListener = new ButtonSelectionAdapter();
  }

  protected void addListeners() {
    m_button.addSelectionListener(m_selectionListener);
    m_listening = true;
  }

  private class ButtonSelectionAdapter extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      if (m_listening) {
        set();
      }
    }
  }

  protected void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil
        .getAncestorOfClass(ConfigurationEditorPanel.class, m_button);

    if (parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void init(Class parentClass, String elementName) {
    m_helper.init(parentClass, elementName);
  }

  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    m_button.setSelection(booleanValue());
    m_listening = true;
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    m_button.setSelection(false);
  }

  public boolean booleanValue() {
    return isSet() ? m_helper.getBoolean() : m_helper.defaultBoolean();
  }

  public boolean isRequired() {
    return m_helper.isRequired();
  }

  public boolean isSet() {
    return m_helper.isSet();
  }

  public void set() {
    m_listening = false;
    boolean isSelected = m_button.getSelection();

    if (isSelected == m_helper.defaultBoolean()) {
      unset();
    } else {
      String s = Boolean.toString(isSelected);

      ensureXmlObject();
      m_helper.set(s);
      m_button.setSelection(isSelected);
    }
    m_listening = true;
  }

  public void unset() {
    if (!isRequired()) {
      m_listening = false;
      m_helper.unset();
      m_button.setSelection(m_helper.defaultBoolean());
      m_listening = true;
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }

  public void setSelection(boolean value) {
    m_button.setSelection(value);
  }

  public boolean getSelection() {
    return m_button.getSelection();
  }

  public void setText(String text) {
    m_button.setText(text);
  }

  public String getText() {
    return m_button.getText();
  }

  public void setEnabled(boolean enabled) {
    m_button.setEnabled(enabled);
  }

  public boolean isEnabled() {
    return m_button.isEnabled();
  }
}
