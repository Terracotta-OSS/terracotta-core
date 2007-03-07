/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTable;
import com.terracottatech.config.Client;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

// TODO: validate that the repo and modules actually exist (this may require a background thread due to request timeouts
public class ModulesPanel extends XContainer implements TableModelListener {

  private Modules                m_modules;
  private Client                 m_dsoClient;
  private XTable                 m_repositoriesTable;
  private XTable                 m_modulesTable;
  private AddModuleDialog        m_addModulesDialog;
  private RepositoriesTableModel m_repositoriesTableModel;
  private ModulesTableModel      m_modulesTableModel;
  private Button                 m_repoAddButton;
  private Button                 m_repoRemoveButton;
  private Button                 m_moduleAddButton;
  private Button                 m_moduleRemoveButton;
  private ActionListener         m_repoAddListener;
  private ActionListener         m_repoRemoveListener;
  private ActionListener         m_moduleAddListener;
  private ActionListener         m_moduleRemoveListener;
  private ListSelectionListener  m_repoSelectionListener;
  private ListSelectionListener  m_moduleSelectionListener;

  public ModulesPanel() {
    super();
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_repositoriesTable = (XTable) findComponent("RepositoriesTable");
    m_repositoriesTable.setModel(m_repositoriesTableModel = new RepositoriesTableModel());

    m_modulesTable = (XTable) findComponent("ModulesTable");
    m_modulesTable.setModel(m_modulesTableModel = new ModulesTableModel());
    m_addModulesDialog = new AddModuleDialog();
    m_moduleAddListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String[] module = m_addModulesDialog.prompt();
        if (module != null) {
          internalAddModule(module[0], module[1]);
        }
      }
    };

    m_moduleRemoveButton = (Button) findComponent("RemoveModuleButton");
    m_moduleRemoveListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Modules modules = ensureModulesElement();
        int[] selection = m_modulesTable.getSelectedRows();
        for (int i = selection.length - 1; i >= 0; i--) {
          modules.removeModule(selection[i]);
        }
        m_modulesTableModel.removeRows(selection);
      }
    };

    m_repoAddButton = (Button) findComponent("AddRepositoryButton");
    m_repoAddListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String repoLocation = JOptionPane.showInputDialog("Repository Location (URL)");
        if (repoLocation != null && !(repoLocation = repoLocation.trim()).equals("")) {
          internalAddRepositoryLocation(repoLocation);
        }
      }
    };

    m_repoRemoveButton = (Button) findComponent("RemoveRepositoryButton");
    m_repoRemoveListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Modules modules = ensureModulesElement();
        int[] selection = m_repositoriesTable.getSelectedRows();
        for (int i = selection.length - 1; i >= 0; i--) {
          modules.removeRepository(selection[i]);
        }
        m_repositoriesTableModel.removeRows(selection);
      }
    };

    m_repoSelectionListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int[] sel = m_repositoriesTable.getSelectedRows();
          m_repoRemoveButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };
    m_moduleSelectionListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int[] sel = m_modulesTable.getSelectedRows();
          m_moduleRemoveButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };

    m_moduleAddButton = (Button) findComponent("AddModuleButton");
  }

  private Modules ensureModulesElement() {
    if (m_modules == null) {
      ensureXmlObject();
    }
    return m_modules;
  }

  // make sure parent exists
  public void ensureXmlObject() {
    if (m_modules == null) {
      removeListeners();
      m_modules = m_dsoClient.addNewModules();
      updateChildren();
      addListeners();
    }
  }

  // match table to xmlbeans
  private void updateChildren() {
    m_modulesTableModel.clear();
    m_repositoriesTableModel.clear();
    if (m_modules != null) {
      String[] repos = m_modules.getRepositoryArray();
      for (int i = 0; i < repos.length; i++) {
        m_repositoriesTableModel.addRepoLocation(repos[i]);
      }
      Module[] modules = m_modules.getModuleArray();
      for (int i = 0; i < modules.length; i++) {
        m_modulesTableModel.addModule(modules[i].getName(), modules[i].getVersion());
      }
    }
  }

  public void tableChanged(TableModelEvent e) {
    syncModel();
  }

  public void setup(Client dsoClient) {
    setEnabled(true);
    removeListeners();
    m_dsoClient = dsoClient;
    m_modules = (m_dsoClient != null) ? m_dsoClient.getModules() : null;
    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();
    m_dsoClient = null;
    m_repositoriesTableModel.clear();
    setEnabled(false);
  }

  private void removeListeners() {
    m_modulesTableModel.removeTableModelListener(this);
    m_repositoriesTableModel.removeTableModelListener(this);
    m_repositoriesTable.getSelectionModel().removeListSelectionListener(m_repoSelectionListener);
    m_repoAddButton.removeActionListener(m_repoAddListener);
    m_repoRemoveButton.removeActionListener(m_repoRemoveListener);
    m_modulesTable.getSelectionModel().removeListSelectionListener(m_moduleSelectionListener);
    m_moduleAddButton.removeActionListener(m_moduleAddListener);
    m_moduleRemoveButton.removeActionListener(m_moduleRemoveListener);
  }

  private void addListeners() {
    m_modulesTableModel.addTableModelListener(this);
    m_repositoriesTableModel.addTableModelListener(this);
    m_repositoriesTable.getSelectionModel().addListSelectionListener(m_repoSelectionListener);
    m_repoAddButton.addActionListener(m_repoAddListener);
    m_repoRemoveButton.addActionListener(m_repoRemoveListener);
    m_modulesTable.getSelectionModel().addListSelectionListener(m_moduleSelectionListener);
    m_moduleAddButton.addActionListener(m_moduleAddListener);
    m_moduleRemoveButton.addActionListener(m_moduleRemoveListener);
  }

  private void internalAddRepositoryLocation(String repoLocation) {
    ensureModulesElement().addRepository(repoLocation);
    m_repositoriesTableModel.addRepoLocation(repoLocation);
    int row = m_repositoriesTableModel.getRowCount() - 1;
    m_repositoriesTable.setRowSelectionInterval(row, row);
  }

  private void internalAddModule(String name, String version) {
    Module module = ensureModulesElement().addNewModule();
    module.setName(name);
    module.setVersion(version);
    m_modulesTableModel.addModule(name, version);
    int row = m_modulesTableModel.getRowCount() - 1;
    m_modulesTable.setRowSelectionInterval(row, row);
  }

  private void syncModel() {
    if (!hasAnySet() && m_dsoClient.getModules() != null) {
      m_dsoClient.unsetModules();
      m_modules = null;
    }
    SessionIntegratorFrame frame = (SessionIntegratorFrame) getAncestorOfClass(SessionIntegratorFrame.class);
    frame.modelChanged();
  }

  public boolean hasAnySet() {
    return m_modules != null && (m_modules.sizeOfRepositoryArray() > 0 || m_modules.sizeOfModuleArray() > 0);
  }

  // --------------------------------------------------------------------------------

  class RepositoriesTableModel extends DefaultTableModel {
    RepositoriesTableModel() {
      super();
      setColumnIdentifiers(new String[] { "Location" });
    }

    void clear() {
      setRowCount(0);
    }

    void addRepoLocation(String typeName) {
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
      m_modules.addRepository((String) value);
      super.setValueAt(value, row, col);
    }

    void removeRows(int[] rows) {
      removeTableModelListener(ModulesPanel.this);
      for (int i = rows.length - 1; i >= 0; i--) {
        removeRow(rows[i]);
      }
      addTableModelListener(ModulesPanel.this);
      syncModel();
    }
  }

  // --------------------------------------------------------------------------------

  class ModulesTableModel extends DefaultTableModel {
    ModulesTableModel() {
      super();
      setColumnIdentifiers(new String[] { "Name", "Version" });
    }

    void clear() {
      setRowCount(0);
    }

    void addModule(String name, String version) {
      addRow(new Object[] { name, version });
    }

    int indexOf(String typeName) {
      int count = getRowCount();
      for (int i = 0; i < count; i++) {
        if (((String) getValueAt(i, 0)).equals(typeName)) { return i; }
      }
      return -1;
    }

    public void setValueAt(Object value, int row, int col) {
      Module module = m_modules.addNewModule();
      if (col == 0) switch (col) {
        case 0:
          module.setName((String) value);
          break;
        case 1:
          module.setVersion((String) value);
          break;

        default:
          break;
      }
      super.setValueAt(value, row, col);
    }

    void removeRows(int[] rows) {
      removeTableModelListener(ModulesPanel.this);
      for (int i = rows.length - 1; i >= 0; i--) {
        removeRow(rows[i]);
      }
      addTableModelListener(ModulesPanel.this);
      syncModel();
    }
  }
}
