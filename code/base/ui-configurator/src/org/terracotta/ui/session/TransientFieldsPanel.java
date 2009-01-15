/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TransientFields;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class TransientFieldsPanel extends XContainer implements TableModelListener {
  private DsoApplication        dsoApp;
  private TransientFields       transientFields;
  private XTable                transientTable;
  private TransientTableModel   transientTableModel;
  private XButton               addButton;
  private ActionListener        addListener;
  private XButton               removeButton;
  private ActionListener        removeListener;
  private ListSelectionListener transientsListener;

  public TransientFieldsPanel() {
    super(new BorderLayout());

    transientTable = new XTable();
    transientTable.setModel(transientTableModel = new TransientTableModel());
    transientsListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int[] sel = transientTable.getSelectedRows();
          removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };
    add(new XScrollPane(transientTable));

    XContainer rightPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addButton = new XButton("Add");
    addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String field = JOptionPane.showInputDialog("Transient field", "");

        if (field != null) {
          field = field.trim();
          if (field != null && field.length() > 0 && !isTransient(field)) {
            internalAddTransient(field);
          }
        }
      }
    };
    rightPanel.add(addButton, gbc);
    gbc.gridy++;

    removeButton = new XButton("Remove");
    removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        TransientFields tf = ensureTransientFields();
        int[] selection = transientTable.getSelectedRows();

        for (int i = selection.length - 1; i >= 0; i--) {
          tf.removeFieldName(selection[i]);
        }
        syncModel();
      }
    };
    rightPanel.add(removeButton, gbc);

    add(rightPanel, BorderLayout.EAST);
  }

  public boolean hasAnySet() {
    return transientFields != null && transientFields.sizeOfFieldNameArray() > 0;
  }

  private TransientFields ensureTransientFields() {
    if (transientFields == null) {
      ensureXmlObject();
    }
    return transientFields;
  }

  public void ensureXmlObject() {
    if (transientFields == null) {
      removeListeners();
      transientFields = dsoApp.addNewTransientFields();
      updateChildren();
      addListeners();
    }
  }

  private void syncModel() {
    if (!hasAnySet() && dsoApp.getTransientFields() != null) {
      dsoApp.unsetTransientFields();
      transientFields = null;
    }
    SessionIntegratorFrame frame = (SessionIntegratorFrame) SwingUtilities
        .getAncestorOfClass(SessionIntegratorFrame.class, this);
    if (frame != null) {
      frame.modelChanged();
    }
  }

  private void addListeners() {
    transientTableModel.addTableModelListener(this);
    transientTable.getSelectionModel().addListSelectionListener(transientsListener);
    addButton.addActionListener(addListener);
    removeButton.addActionListener(removeListener);
  }

  private void removeListeners() {
    transientTableModel.removeTableModelListener(this);
    transientTable.getSelectionModel().removeListSelectionListener(transientsListener);
    addButton.removeActionListener(addListener);
    removeButton.removeActionListener(removeListener);
  }

  private void updateChildren() {
    transientTableModel.clear();

    if (transientFields != null) {
      String[] transients = transientFields.getFieldNameArray();
      for (int i = 0; i < transients.length; i++) {
        transientTableModel.addField(transients[i]);
      }
    }
  }

  public void setup(DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    this.dsoApp = dsoApp;
    transientFields = dsoApp != null ? dsoApp.getTransientFields() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    dsoApp = null;
    transientFields = null;

    transientTableModel.clear();

    setEnabled(false);
  }

  class TransientTableModel extends DefaultTableModel {
    TransientTableModel() {
      super();
      setColumnIdentifiers(new String[] { "Transient fields" });
    }

    void clear() {
      setRowCount(0);
    }

    void setTransientFields(TransientFields fields) {
      clear();

      if (fields != null) {
        int count = fields.sizeOfFieldNameArray();
        for (int i = 0; i < count; i++) {
          addField(fields.getFieldNameArray(i));
        }
      }
    }

    void addField(String fieldName) {
      addRow(new Object[] { fieldName });
    }

    int indexOf(String fieldName) {
      int count = getRowCount();
      for (int i = 0; i < count; i++) {
        if (((String) getValueAt(i, 0)).equals(fieldName)) { return i; }
      }
      return -1;
    }

    public void setValueAt(Object value, int row, int col) {
      transientFields.setFieldNameArray(row, (String) value);
      super.setValueAt(value, row, col);
    }
  }

  public void tableChanged(TableModelEvent tme) {
    syncModel();
  }

  private void internalAddTransient(String fieldName) {
    ensureTransientFields().addFieldName(fieldName);
    syncModel();
    int row = transientTableModel.getRowCount() - 1;
    transientTable.setRowSelectionInterval(row, row);
  }

  private void internalRemoveTransient(String fieldName) {
    int row = transientTableModel.indexOf(fieldName);

    if (row >= 0) {
      ensureTransientFields().removeFieldName(row);
      syncModel();
      if (row > 0) {
        row = Math.min(transientTableModel.getRowCount() - 1, row);
        transientTable.setRowSelectionInterval(row, row);
      }
    }
  }

  public boolean isTransient(String fieldName) {
    TransientFields transients = ensureTransientFields();
    int count = transients.sizeOfFieldNameArray();

    for (int i = 0; i < count; i++) {
      if (fieldName.equals(transients.getFieldNameArray(i))) { return true; }
    }

    return false;
  }

  public void ensureTransient(String fieldName) {
    if (!isTransient(fieldName)) {
      internalAddTransient(fieldName);
    }
  }

  public void ensureNotTransient(String fieldName) {
    if (isTransient(fieldName)) {
      internalRemoveTransient(fieldName);
    }
  }
}
