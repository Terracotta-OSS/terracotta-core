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

public class TextComponentHelper extends XPopupListener implements CaretListener {
  protected JTextComponent  component;
  protected CutAction       cutAction;
  protected CopyAction      copyAction;
  protected PasteAction     pasteAction;
  protected ClearAction     clearAction;
  protected SelectAllAction selectAllAction;

  public TextComponentHelper() {
    super();
  }

  public TextComponentHelper(JTextComponent component) {
    this();
    setTarget(component);
  }

  protected void setTarget(JTextComponent component) {
    if (component != null) {
      component.removeCaretListener(this);
    }
    super.setTarget(this.component = component);
    if (component != null) {
      component.addCaretListener(this);
    }
  }

  public JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu("TextComponent Actions");

    if (component.isEditable()) {
      addCutAction(popup);
    }
    addCopyAction(popup);
    if (component.isEditable()) {
      addPasteAction(popup);
      addClearAction(popup);
    }
    addSelectAllAction(popup);

    return popup;
  }

  protected void addCutAction(JPopupMenu popup) {
    popup.add(cutAction = new CutAction());
  }

  public Action getCutAction() {
    return cutAction;
  }

  protected void addCopyAction(JPopupMenu popup) {
    popup.add(copyAction = new CopyAction());
  }

  public Action getCopyAction() {
    return copyAction;
  }

  protected void addPasteAction(JPopupMenu popup) {
    popup.add(pasteAction = new PasteAction());
  }

  public Action getPasteAction() {
    return pasteAction;
  }

  protected void addClearAction(JPopupMenu popup) {
    popup.add(clearAction = new ClearAction());
  }

  public Action getClearAction() {
    return clearAction;
  }

  protected void addSelectAllAction(JPopupMenu popup) {
    popup.addSeparator();
    popup.add(selectAllAction = new SelectAllAction());
  }

  private class CutAction extends XAbstractAction {
    protected CutAction() {
      super("Cut");
      String uri = "/com/tc/admin/icons/cut_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      component.cut();
    }
  }

  private class CopyAction extends XAbstractAction {
    protected CopyAction() {
      super("Copy");
      String uri = "/com/tc/admin/icons/copy_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      component.copy();
    }
  }

  private class PasteAction extends XAbstractAction {
    protected PasteAction() {
      super("Paste");
      String uri = "/com/tc/admin/icons/paste_edit.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      component.paste();
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
        Document doc = component.getDocument();
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
      final JScrollPane scroller = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, component);
      Point scrollLoc = null;
      if (scroller != null) {
        scrollLoc = scroller.getViewport().getViewPosition();
      }
      component.requestFocusInWindow();
      component.selectAll();
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
    return (component.getSelectionStart() - component.getSelectionEnd()) != 0;
  }

  private void testEnableMenuItems() {
    boolean hasSelectionRange = hasSelectionRange();
    boolean editable = component.isEditable();
    boolean haveContent = component.getDocument().getLength() > 0;

    if (cutAction != null) {
      cutAction.setEnabled(editable && hasSelectionRange);
    }
    if (copyAction != null) {
      copyAction.setEnabled(hasSelectionRange);
    }
    if (pasteAction != null) {
      pasteAction.setEnabled(editable);
    }
    if (clearAction != null) {
      clearAction.setEnabled(haveContent);
    }
    if (selectAllAction != null) {
      selectAllAction.setEnabled(haveContent);
    }
  }

  public void caretUpdate(CaretEvent e) {
    testEnableMenuItems();
  }
}
