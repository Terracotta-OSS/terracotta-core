/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.List;

import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class XList extends List implements ListSelectionListener {
  protected XPopupListener m_popupListener;
  protected DeleteAction   m_deleteAction;
  
  public XList() {
    super();
    m_popupListener = new XPopupListener(this);
    m_popupListener.setPopupMenu(createPopup());
    addListSelectionListener(this);
  }
  
  public JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu("List Actions");
    
    popup.add(m_deleteAction = createDeleteAction());
    
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
      
      if(model instanceof DefaultListModel) {
        int[] rows = getSelectedIndices();
      
        for(int i = rows.length-1; i >= 0; i--) {
          ((DefaultListModel)model).remove(rows[i]);
        }
      }
    }
  }

  public void valueChanged(ListSelectionEvent e) {
    m_deleteAction.setEnabled(!isSelectionEmpty());
  }
}
