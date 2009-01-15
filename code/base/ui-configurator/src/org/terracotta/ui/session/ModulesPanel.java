/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.xmlbeans.XmlString;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable;
import com.terracottatech.config.Client;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

// TODO: validate that the repo and modules actually exist (this may require a background thread due to request timeouts
// TODO: provide module repo selection dialog, like in the DSO Eclipse plug-in

public class ModulesPanel extends XContainer implements TableModelListener {

  private Modules                modules;
  private Client                 dsoClient;
  private XTable                 repositoriesTable;
  private XTable                 modulesTable;
  private AddModuleDialog        addModulesDialog;
  private RepositoriesTableModel repositoriesTableModel;
  private ModulesTableModel      modulesTableModel;
  private XButton                repoAddButton;
  private XButton                repoRemoveButton;
  private XButton                moduleAddButton;
  private XButton                moduleRemoveButton;
  private ActionListener         repoAddListener;
  private ActionListener         repoRemoveListener;
  private ActionListener         moduleAddListener;
  private ActionListener         moduleRemoveListener;
  private ListSelectionListener  repoSelectionListener;
  private ListSelectionListener  moduleSelectionListener;

  public ModulesPanel() {
    super(new GridLayout(2, 1));

    XContainer repoPanel = new XContainer(new BorderLayout());
    repositoriesTable = new XTable();
    repositoriesTable.setModel(repositoriesTableModel = new RepositoriesTableModel());
    repoPanel.add(new XScrollPane(repositoriesTable));

    XContainer rightPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    repoAddButton = new XButton("Add...");
    repoAddListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String repoLocation = JOptionPane.showInputDialog("Repository Location (URL)");
        if (repoLocation != null && !(repoLocation = repoLocation.trim()).equals("")) {
          internalAddRepositoryLocation(repoLocation);
        }
      }
    };
    rightPanel.add(repoAddButton, gbc);
    gbc.gridy++;

    repoRemoveButton = new XButton("Remove");
    repoRemoveListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Modules theModules = ensureModulesElement();
        int[] selection = repositoriesTable.getSelectedRows();
        for (int i = selection.length - 1; i >= 0; i--) {
          theModules.removeRepository(selection[i]);
        }
        syncModel();
      }
    };
    rightPanel.add(repoRemoveButton, gbc);
    repoPanel.add(rightPanel, BorderLayout.EAST);

    repoPanel.setBorder(BorderFactory.createTitledBorder("Repositories"));
    add(repoPanel);

    XContainer modulesPanel = new XContainer(new BorderLayout());
    modulesTable = new XTable();
    modulesTable.setModel(modulesTableModel = new ModulesTableModel());
    moduleSelectionListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int[] sel = modulesTable.getSelectedRows();
          moduleRemoveButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };
    modulesPanel.add(new XScrollPane(modulesTable));

    rightPanel = new XContainer(new GridBagLayout());
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    moduleAddButton = new XButton("Add...");
    addModulesDialog = new AddModuleDialog();
    moduleAddListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String[] module = addModulesDialog.prompt();
        if (module != null) {
          internalAddModule(module[0], module[1]);
        }
      }
    };
    rightPanel.add(moduleAddButton, gbc);
    gbc.gridy++;

    moduleRemoveButton = new XButton("Remove");
    moduleRemoveListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Modules theModules = ensureModulesElement();
        int[] selection = modulesTable.getSelectedRows();
        for (int i = selection.length - 1; i >= 0; i--) {
          theModules.removeModule(selection[i]);
        }
        syncModel();
      }
    };
    rightPanel.add(moduleRemoveButton, gbc);
    modulesPanel.add(rightPanel, BorderLayout.EAST);

    modulesPanel.setBorder(BorderFactory.createTitledBorder("Modules"));
    add(modulesPanel);
  }

  private Modules ensureModulesElement() {
    if (modules == null) {
      ensureXmlObject();
    }
    return modules;
  }

  // make sure parent exists
  public void ensureXmlObject() {
    if (modules == null) {
      removeListeners();
      modules = dsoClient.addNewModules();
      updateChildren();
      addListeners();
    }
  }

  // match table to xmlbeans
  private void updateChildren() {
    modulesTableModel.clear();
    repositoriesTableModel.clear();
    if (modules != null) {
      repositoriesTableModel.set(modules.xgetRepositoryArray());
      modulesTableModel.set(modules.getModuleArray());
    } else {
      repositoriesTableModel.fireTableDataChanged();
      modulesTableModel.fireTableDataChanged();
    }
  }

  public void tableChanged(TableModelEvent e) {
    syncModel();
  }

  public void setup(Client dsoClient) {
    setEnabled(true);
    removeListeners();
    this.dsoClient = dsoClient;
    modules = (dsoClient != null) ? dsoClient.getModules() : null;
    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    dsoClient = null;
    repositoriesTableModel.clear();
    setEnabled(false);
  }

  private void removeListeners() {
    modulesTableModel.removeTableModelListener(this);
    repositoriesTableModel.removeTableModelListener(this);
    repositoriesTable.getSelectionModel().removeListSelectionListener(repoSelectionListener);
    repoAddButton.removeActionListener(repoAddListener);
    repoRemoveButton.removeActionListener(repoRemoveListener);
    modulesTable.getSelectionModel().removeListSelectionListener(moduleSelectionListener);
    moduleAddButton.removeActionListener(moduleAddListener);
    moduleRemoveButton.removeActionListener(moduleRemoveListener);
  }

  private void addListeners() {
    modulesTableModel.addTableModelListener(this);
    repositoriesTableModel.addTableModelListener(this);
    repositoriesTable.getSelectionModel().addListSelectionListener(repoSelectionListener);
    repoAddButton.addActionListener(repoAddListener);
    repoRemoveButton.addActionListener(repoRemoveListener);
    modulesTable.getSelectionModel().addListSelectionListener(moduleSelectionListener);
    moduleAddButton.addActionListener(moduleAddListener);
    moduleRemoveButton.addActionListener(moduleRemoveListener);
  }

  private void internalAddRepositoryLocation(String repoLocation) {
    XmlString elem = ensureModulesElement().addNewRepository();
    elem.setStringValue(repoLocation);
    syncModel();
    int row = repositoriesTableModel.getRowCount() - 1;
    repositoriesTable.setRowSelectionInterval(row, row);
  }

  private void internalAddModule(String name, String version) {
    Module module = ensureModulesElement().addNewModule();
    module.setName(name);
    module.setVersion(version);
    syncModel();
    int row = modulesTableModel.getRowCount() - 1;
    modulesTable.setRowSelectionInterval(row, row);
  }

  private void syncModel() {
    if (!hasAnySet() && dsoClient.getModules() != null) {
      dsoClient.unsetModules();
      modules = null;
    }
    SessionIntegratorFrame frame = (SessionIntegratorFrame) SwingUtilities
        .getAncestorOfClass(SessionIntegratorFrame.class, this);
    if (frame != null) {
      frame.modelChanged();
    }
  }

  public boolean hasAnySet() {
    return modules != null && (modules.sizeOfRepositoryArray() > 0 || modules.sizeOfModuleArray() > 0);
  }

  // --------------------------------------------------------------------------------

  class RepositoriesTableModel extends XObjectTableModel {
    RepositoriesTableModel() {
      super(XmlString.class, new String[] { "StringValue" }, new String[] { "Location" });
    }

    public void setValueAt(Object value, int row, int col) {
      modules.setRepositoryArray(row, (String) value);
      super.setValueAt(value, row, col);
    }
  }

  // --------------------------------------------------------------------------------

  private static final String[] MODULE_FIELDS = { "Name", "Version" };

  class ModulesTableModel extends XObjectTableModel {
    ModulesTableModel() {
      super(Module.class, MODULE_FIELDS, MODULE_FIELDS);
    }
  }
}
