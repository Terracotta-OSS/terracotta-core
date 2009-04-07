/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.terracotta.dso.editors.ConfigurationEditorPanel;
import org.terracotta.ui.util.SWTUtil;

public class XmlStringEnumCombo implements XmlObjectHolder {
  private final XmlObjectHolderHelper m_helper;
  private final Combo                 m_combo;
  private final ModifyListener        m_modifyListener;
  private final KeyListener           m_keyListener;
  private boolean                     m_listening;
  private StringEnumAbstractBase[]    m_enumValues;

  public static XmlStringEnumCombo newInstance(Combo combo) {
    XmlStringEnumCombo result = new XmlStringEnumCombo(combo);
    result.addListeners();
    return result;
  }

  protected void addListeners() {
    m_combo.addModifyListener(m_modifyListener);
    m_combo.addKeyListener(m_keyListener);
    m_listening = true;
  }

  protected XmlStringEnumCombo(Combo combo) {
    m_helper = new XmlObjectHolderHelper();
    m_combo = combo;
    m_modifyListener = new ComboModifyListener();
    m_keyListener = new ComboKeyAdapter();
  }

  private class ComboModifyListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      if (m_listening) {
        set();
      }
    }
  }

  private class ComboKeyAdapter extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (!m_listening) return;
      switch (e.keyCode) {
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
  }

  protected void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil
        .getAncestorOfClass(ConfigurationEditorPanel.class, m_combo);

    if (parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void init(Class parentType, String elementName) {
    m_helper.init(parentType, elementName);
    m_enumValues = m_helper.getEnumValues();
    for (int i = 0; i < m_enumValues.length; i++) {
      m_combo.add(m_enumValues[i].toString());
    }
  }

  private int valueIndex(StringEnumAbstractBase enumValue) {
    for (int i = 0; i < m_enumValues.length; i++) {
      if (enumValue.equals(m_enumValues[i])) return i;
    }
    return -1;
  }

  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    m_combo.select(valueIndex(enumValue()));
    m_listening = true;
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
  }

  public String getSelectedEnumString() {
    return m_enumValues[m_combo.getSelectionIndex()].toString();
  }

  public int getSelectedEnumInt() {
    return m_enumValues[m_combo.getSelectionIndex()].intValue();
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
    String s = getSelectedEnumString();
    ensureXmlObject();
    m_helper.set(s);
  }

  public void unset() {
    if (!isRequired()) {
      m_listening = false;
      m_helper.unset();
      m_combo.select(valueIndex(m_helper.defaultEnumValue()));
      m_listening = true;
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }
}
