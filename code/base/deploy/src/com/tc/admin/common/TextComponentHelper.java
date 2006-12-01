/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class TextComponentHelper extends XPopupListener
  implements CaretListener
{
  protected JTextComponent m_component;
  protected CutAction      m_cutAction;
  protected CopyAction     m_copyAction;
  protected PasteAction    m_pasteAction;
  protected ClearAction    m_clearAction;
  
  public TextComponentHelper() {
    super();
  }
  
  public TextComponentHelper(JTextComponent component) {
    this();
    setTarget(component);
  }

  protected void setTarget(JTextComponent component) {
    if(m_component != null) {
      m_component.removeCaretListener(this);
    }

    super.setTarget(m_component = component);

    if(component != null) {
      component.addCaretListener(this);
    }
  }
  
  public JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu("TextComponent Actions");
    
    addCutAction(popup);
    addCopyAction(popup);
    addPasteAction(popup);
    addClearAction(popup);
    
    return popup;
  }

  protected void addCutAction(JPopupMenu popup) {
    popup.add(m_cutAction = new CutAction());
  }
  
  protected void addCopyAction(JPopupMenu popup) {
    popup.add(m_copyAction = new CopyAction());
  }

  protected void addPasteAction(JPopupMenu popup) {
    popup.add(m_pasteAction = new PasteAction());
  }

  protected void addClearAction(JPopupMenu popup) {
    popup.add(m_clearAction = new ClearAction());
  }

  protected class CutAction extends XAbstractAction {
    protected CutAction() {
      super("Cut");
      String uri = "/com/tc/admin/icons/cut_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.cut();
    }
  }
  
  protected class CopyAction extends XAbstractAction {
    protected CopyAction() {
      super("Copy");
      String uri = "/com/tc/admin/icons/copy_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.copy();
    }
  }
  
  protected class PasteAction extends XAbstractAction {
    protected PasteAction() {
      super("Paste");
      String uri = "/com/tc/admin/icons/paste_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.paste();
    }
  }

  protected class ClearAction extends XAbstractAction {
    protected ClearAction() {
      super("Clear");
      String uri = "/com/tc/admin/icons/clear_co.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      Document doc = m_component.getDocument();
      
      try {
        doc.remove(0, doc.getLength());
      } catch(BadLocationException ble) {/**/}
    }
  }

  public boolean hasSelectionRange() {
    return (m_component.getSelectionStart()-m_component.getSelectionEnd()) != 0;
  }
  
  private void testEnableMenuItems() {
    boolean hasSelectionRange = hasSelectionRange();
    boolean editable          = m_component.isEditable();
    
    if(m_cutAction != null) {
      m_cutAction.setEnabled(editable && hasSelectionRange);
    }

    if(m_copyAction != null) {
      m_copyAction.setEnabled(hasSelectionRange);
    }
    
    if(m_pasteAction != null) {
      m_pasteAction.setEnabled(editable);
    }
    
    if(m_clearAction != null) {
      m_clearAction.setEnabled(m_component.getDocument().getLength() > 0);
    }
  }
  
  public void caretUpdate(CaretEvent e) {
    testEnableMenuItems();
  }
}
