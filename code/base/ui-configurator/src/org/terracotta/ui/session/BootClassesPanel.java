/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.DsoApplication;

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

public class BootClassesPanel extends XContainer implements TableModelListener {
  private DsoApplication           dsoApp;
  private AdditionalBootJarClasses bootClasses;
  private XTable                   bootClassesTable;
  private BootClassTableModel      bootClassesTableModel;
  private XButton                  addButton;
  private ActionListener           addListener;
  private XButton                  removeButton;
  private ActionListener           removeListener;
  private ListSelectionListener    bootClassesListener;

  public BootClassesPanel() {
    super(new BorderLayout());

    bootClassesTable = new XTable();
    bootClassesTable.setModel(bootClassesTableModel = new BootClassTableModel());
    bootClassesListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int[] sel = bootClassesTable.getSelectedRows();
          removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };
    add(new XScrollPane(bootClassesTable));

    XContainer rightPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addButton = new XButton("Add");
    addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String bootType = JOptionPane.showInputDialog("Boot class");

        if (bootType != null) {
          bootType = bootType.trim();
          if (bootType != null && bootType.length() > 0) {
            internalAddBootClass(bootType);
          }
        }
      }
    };
    rightPanel.add(addButton, gbc);
    gbc.gridy++;

    removeButton = new XButton("Remove");
    removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        AdditionalBootJarClasses abjc = ensureAdditionalBootClasses();
        int[] selection = bootClassesTable.getSelectedRows();

        for (int i = selection.length - 1; i >= 0; i--) {
          abjc.removeInclude(selection[i]);
        }
        syncModel();
      }
    };
    rightPanel.add(removeButton, gbc);

    add(rightPanel, BorderLayout.EAST);
  }

  public boolean hasAnySet() {
    return bootClasses != null && bootClasses.sizeOfIncludeArray() > 0;
  }

  private AdditionalBootJarClasses ensureAdditionalBootClasses() {
    if (bootClasses == null) {
      ensureXmlObject();
    }
    return bootClasses;
  }

  // make sure parent exists
  public void ensureXmlObject() {
    if (bootClasses == null) {
      removeListeners();
      bootClasses = dsoApp.addNewAdditionalBootJarClasses();
      updateChildren();
      addListeners();
    }
  }

  // write xml and remove if not needed
  private void syncModel() {
    if (!hasAnySet() && dsoApp.getAdditionalBootJarClasses() != null) {
      dsoApp.unsetAdditionalBootJarClasses();
      bootClasses = null;
    }
    SessionIntegratorFrame frame = (SessionIntegratorFrame) SwingUtilities
        .getAncestorOfClass(SessionIntegratorFrame.class, this);
    if (frame != null) {
      frame.modelChanged();
    }
  }

  private void addListeners() {
    bootClassesTableModel.addTableModelListener(this);
    bootClassesTable.getSelectionModel().addListSelectionListener(bootClassesListener);
    addButton.addActionListener(addListener);
    removeButton.addActionListener(removeListener);
  }

  private void removeListeners() {
    bootClassesTableModel.removeTableModelListener(this);
    bootClassesTable.getSelectionModel().removeListSelectionListener(bootClassesListener);
    addButton.removeActionListener(addListener);
    removeButton.removeActionListener(removeListener);
  }

  // match table to xmlbeans
  private void updateChildren() {
    bootClassesTableModel.clear();

    if (bootClasses != null) {
      String[] bootTypes = bootClasses.getIncludeArray();
      for (int i = 0; i < bootTypes.length; i++) {
        bootClassesTableModel.addBootClass(bootTypes[i]);
      }
    }
  }

  public void setup(DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    this.dsoApp = dsoApp;
    bootClasses = dsoApp != null ? dsoApp.getAdditionalBootJarClasses() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    dsoApp = null;
    bootClasses = null;

    bootClassesTableModel.clear();

    setEnabled(false);
  }

  class BootClassTableModel extends DefaultTableModel {
    BootClassTableModel() {
      super();
      setColumnIdentifiers(new String[] { "Boot Classes" });
    }

    void clear() {
      setRowCount(0);
    }

    void setBootClasses(AdditionalBootJarClasses bootClasses) {
      clear();

      if (bootClasses != null) {
        int count = bootClasses.sizeOfIncludeArray();
        for (int i = 0; i < count; i++) {
          addBootClass(bootClasses.getIncludeArray(i));
        }
      }
    }

    void addBootClass(String typeName) {
      addRow(new Object[] { typeName });
    }

    int indexOf(String typeName) {
      int count = getRowCount();

      for (int i = 0; i < count; i++) {
        if (((String) getValueAt(i, 0)).equals(typeName)) { return i; }
      }

      return -1;
    }

    public void setValueAt(Object value, int row, int col) {
      bootClasses.setIncludeArray(row, (String) value);
      super.setValueAt(value, row, col);
    }
  }

  public void tableChanged(TableModelEvent tme) {
    syncModel();
  }

  private void internalAddBootClass(String typeName) {
    ensureAdditionalBootClasses().addInclude(typeName);
    syncModel();
    int row = bootClassesTableModel.getRowCount() - 1;
    bootClassesTable.setRowSelectionInterval(row, row);
  }

  private void internalRemoveBootClass(String typeName) {
    int row = bootClassesTableModel.indexOf(typeName);

    if (row >= 0) {
      ensureAdditionalBootClasses().removeInclude(row);
      syncModel();
      if (row > 0) {
        row = Math.min(bootClassesTableModel.getRowCount() - 1, row);
        bootClassesTable.setRowSelectionInterval(row, row);
      }
    }
  }

  public boolean isBootClass(String typeName) {
    AdditionalBootJarClasses theBootClasses = ensureAdditionalBootClasses();
    int count = theBootClasses.sizeOfIncludeArray();

    for (int i = 0; i < count; i++) {
      if (typeName.equals(theBootClasses.getIncludeArray(i))) { return true; }
    }

    return false;
  }

  public void ensureBootClass(String typeName) {
    if (!isBootClass(typeName)) {
      internalAddBootClass(typeName);
    }
  }

  public void ensureNotBootClass(String fieldName) {
    if (isBootClass(fieldName)) {
      internalRemoveBootClass(fieldName);
    }
  }
}
