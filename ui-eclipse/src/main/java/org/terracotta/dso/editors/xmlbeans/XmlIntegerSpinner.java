/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Spinner;
import org.terracotta.dso.editors.ConfigurationEditorPanel;
import org.terracotta.ui.util.SWTUtil;

public class XmlIntegerSpinner implements XmlObjectHolder {
  private final XmlObjectHolderHelper m_helper;
  private final Spinner               m_spinner;
  private final FocusListener         m_focusListener;
  private final SelectionListener     m_selectionListener;
  private final KeyListener           m_keyListener;
  private boolean                     m_listening;

  public static XmlIntegerSpinner newInstance(Spinner spinner) {
    XmlIntegerSpinner result = new XmlIntegerSpinner(spinner);
    result.addListeners();
    return result;
  }

  protected void addListeners() {
    m_spinner.addFocusListener(m_focusListener);
    m_spinner.addSelectionListener(m_selectionListener);
    m_spinner.addKeyListener(m_keyListener);
    m_listening = true;
  }

  protected XmlIntegerSpinner(Spinner spinner) {
    m_helper = new XmlObjectHolderHelper();
    m_spinner = spinner;
    m_focusListener = new SpinnerFocusAdapter();
    m_selectionListener = new SpinnerSelectionAdapter();
    m_keyListener = new SpinnerKeyAdapter();
  }

  private class SpinnerFocusAdapter extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent e) {
      if (m_listening) {
        set();
      }
    }
  }

  private class SpinnerSelectionAdapter extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent se) {
      // If spinner is FocusControl, the focusLost event will handle setting.
      if (m_listening && !m_spinner.isFocusControl()) {
        set();
      }
    }
  }

  private class SpinnerKeyAdapter extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (!m_listening) return;
      switch (e.keyCode) {
        case SWT.F5: {
          unset();
          break;
        }
      }
    }
  }

  protected void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel) SWTUtil
        .getAncestorOfClass(ConfigurationEditorPanel.class, m_spinner);

    if (parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void setup(XmlObject parent) {
    m_listening = false;
    m_helper.setup(parent);
    setSelection(integerValue());
    m_listening = true;
  }

  public void init(Class parentClass, String elementName) {
    m_helper.init(parentClass, elementName);
    setSelection(m_helper.defaultIntegerValue());
    m_spinner.setMinimum(m_helper.minInclusive());
    m_spinner.setMaximum(m_helper.maxInclusive());
    m_spinner.setPageIncrement(1);
  }

  public void tearDown() {
    m_helper.tearDown();
    m_listening = false;
    setSelection(m_helper.defaultIntegerValue());
  }

  public Integer integerValue() {
    return isSet() ? m_helper.getIntegerValue() : m_helper.defaultIntegerValue();
  }

  public boolean isRequired() {
    return m_helper.isRequired();
  }

  public boolean isSet() {
    return m_helper.isSet();
  }

  public void set() {
    m_listening = false;
    int iVal = getSelection();
    if (m_helper.hasDefault() && m_helper.defaultIntegerValue().equals(iVal)) {
      unset();
    } else {
      ensureXmlObject();
      m_helper.set(Integer.toString(iVal));
    }
    m_listening = true;
  }

  public void unset() {
    if (!isRequired()) {
      m_listening = false;
      m_helper.unset();
      Integer iVal = m_helper.defaultIntegerValue();
      setSelection(iVal);
      m_listening = true;
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.addXmlObjectStructureListener(listener);
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    m_helper.removeXmlObjectStructureListener(listener);
  }

  public int getSelection() {
    return m_spinner.getSelection();
  }

  public void setSelection(int value) {
    int curVal = m_spinner.getSelection();
    if (curVal != value) {
      m_spinner.setSelection(value);
    }
  }
}
