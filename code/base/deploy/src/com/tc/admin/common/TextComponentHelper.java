/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

// XXX: DEPRECATED
public class TextComponentHelper extends XPopupListener implements CaretListener {
  protected JTextComponent  m_component;
  protected CutAction       m_cutAction;
  protected CopyAction      m_copyAction;
  protected PasteAction     m_pasteAction;
  protected ClearAction     m_clearAction;
  protected SelectAllAction m_selectAllAction;

  public TextComponentHelper() {
    super();
  }

  public TextComponentHelper(JTextComponent component) {
    this();
    setTarget(component);
  }

  protected void setTarget(JTextComponent component) {
    if (m_component != null) {
      m_component.removeCaretListener(this);
    }

    super.setTarget(m_component = component);

    if (component != null) {
      component.addCaretListener(this);
    }
  }

  public JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu("TextComponent Actions");

    if (m_component.isEditable()) {
      addCutAction(popup);
    }
    addCopyAction(popup);
    if (m_component.isEditable()) {
      addPasteAction(popup);
      addClearAction(popup);
    }
    addSelectAllAction(popup);

    return popup;
  }

  protected void addCutAction(JPopupMenu popup) {
    popup.add(m_cutAction = new CutAction());
  }

  public Action getCutAction() {
    return m_cutAction;
  }

  protected void addCopyAction(JPopupMenu popup) {
    popup.add(m_copyAction = new CopyAction());
  }

  public Action getCopyAction() {
    return m_copyAction;
  }

  protected void addPasteAction(JPopupMenu popup) {
    popup.add(m_pasteAction = new PasteAction());
  }

  public Action getPasteAction() {
    return m_pasteAction;
  }

  protected void addClearAction(JPopupMenu popup) {
    popup.add(m_clearAction = new ClearAction());
  }

  public Action getClearAction() {
    return m_clearAction;
  }

  protected void addSelectAllAction(JPopupMenu popup) {
    popup.addSeparator();
    popup.add(m_selectAllAction = new SelectAllAction());
  }

  private class CutAction extends XAbstractAction {
    protected CutAction() {
      super("Cut");
      String uri = "/com/tc/admin/icons/cut_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.cut();
    }
  }

  private class CopyAction extends XAbstractAction {
    protected CopyAction() {
      super("Copy");
      String uri = "/com/tc/admin/icons/copy_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.copy();
    }
  }

  private class PasteAction extends XAbstractAction {
    protected PasteAction() {
      super("Paste");
      String uri = "/com/tc/admin/icons/paste_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      m_component.paste();
    }
  }

  private class ClearAction extends XAbstractAction {
    protected ClearAction() {
      super("Clear");
      String uri = "/com/tc/admin/icons/clear_co.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      try {
        Document doc = m_component.getDocument();
        doc.remove(0, doc.getLength());
      } catch (BadLocationException ble) {
        throw new AssertionError(ble);
      }
    }
  }

  private class SelectAllAction extends XAbstractAction {
    protected SelectAllAction() {
      super("Select All");

    }

    public void actionPerformed(ActionEvent ae) {
      final JScrollPane scroller = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, m_component);
      Point scrollLoc = null;
      if (scroller != null) {
        scrollLoc = scroller.getViewport().getViewPosition();
      }
      m_component.requestFocusInWindow();
      m_component.selectAll();
      if (scrollLoc != null) {
        final Point loc = scrollLoc;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            scroller.getViewport().setViewPosition(loc);
          }
        });
      }
    }
  }

  public boolean hasSelectionRange() {
    return (m_component.getSelectionStart() - m_component.getSelectionEnd()) != 0;
  }

  private void testEnableMenuItems() {
    boolean hasSelectionRange = hasSelectionRange();
    boolean editable = m_component.isEditable();
    boolean haveContent = m_component.getDocument().getLength() > 0;
    
    if (m_cutAction != null) {
      m_cutAction.setEnabled(editable && hasSelectionRange);
    }

    if (m_copyAction != null) {
      m_copyAction.setEnabled(hasSelectionRange);
    }

    if (m_pasteAction != null) {
      m_pasteAction.setEnabled(editable);
    }

    if (m_clearAction != null) {
      m_clearAction.setEnabled(haveContent);
    }
    
    if(m_selectAllAction != null) {
      m_selectAllAction.setEnabled(haveContent);
    }
  }

  public void caretUpdate(CaretEvent e) {
    testEnableMenuItems();
  }
}
