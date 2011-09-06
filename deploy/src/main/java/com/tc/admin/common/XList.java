/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class XList extends JList implements ListSelectionListener {
  protected XPopupListener popupListener;
  protected DeleteAction   deleteAction;

  public XList() {
    super();
    popupListener = new XPopupListener(this);
    popupListener.setPopupMenu(createPopup());
    addListSelectionListener(this);
    setVisibleRowCount(5);
  }

  public JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu("List Actions");
    popup.add(deleteAction = createDeleteAction());
    return popup;
  }

  protected DeleteAction createDeleteAction() {
    return new DeleteAction();
  }

  protected class DeleteAction extends XAbstractAction {
    protected DeleteAction() {
      super("Delete");
    }

    public void actionPerformed(ActionEvent ae) {
      ListModel model = getModel();
      if (model instanceof DefaultListModel) {
        int[] rows = getSelectedIndices();
        for (int i = rows.length - 1; i >= 0; i--) {
          ((DefaultListModel) model).remove(rows[i]);
        }
      }
    }
  }

  public void valueChanged(ListSelectionEvent e) {
    deleteAction.setEnabled(!isSelectionEmpty());
  }
}
