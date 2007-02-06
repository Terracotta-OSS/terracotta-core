/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTable;
import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.DsoApplication;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class BootClassesPanel extends XContainer
  implements TableModelListener
{
  private DsoApplication           m_dsoApp;
  private AdditionalBootJarClasses m_bootClasses;
  private XTable                   m_bootClassesTable;
  private BootClassTableModel      m_bootClassesTableModel;
  private Button                   m_addButton;
  private ActionListener           m_addListener;
  private Button                   m_removeButton;
  private ActionListener           m_removeListener;
  private ListSelectionListener    m_bootClassesListener;
  
  public BootClassesPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_bootClassesTable = (XTable)findComponent("BootClassesTable");
    m_bootClassesTable.setModel(m_bootClassesTableModel = new BootClassTableModel());
    m_bootClassesListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_bootClassesTable.getSelectedRows();
          m_removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };        
    
    m_addButton = (Button)findComponent("AddBootClassButton");
    m_addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String bootType = JOptionPane.showInputDialog("Boot class");
        
        if(bootType != null) {
          bootType = bootType.trim();
          
          if(bootType != null && bootType.length() > 0) {
            internalAddBootClass(bootType);
          }
        }
      }
    };

    m_removeButton = (Button)findComponent("RemoveBootClassButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        AdditionalBootJarClasses abjc      = ensureAdditionalBootClasses();
        int[]                    selection = m_bootClassesTable.getSelectedRows();
        
        for(int i = selection.length-1; i >= 0; i--) {
          abjc.removeInclude(selection[i]);
        }
        m_bootClassesTableModel.removeRows(selection);
      }
    };
  }

  public boolean hasAnySet() {
    return m_bootClasses != null &&
           m_bootClasses.sizeOfIncludeArray() > 0;
  }
  
  private AdditionalBootJarClasses ensureAdditionalBootClasses() {
    if(m_bootClasses == null) {
      ensureXmlObject();
    }
    return m_bootClasses;
  }
  
  public void ensureXmlObject() {
    if(m_bootClasses == null) {
      removeListeners();
      m_bootClasses = m_dsoApp.addNewAdditionalBootJarClasses();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoApp.getAdditionalBootJarClasses() != null) {
      m_dsoApp.unsetAdditionalBootJarClasses();
      m_bootClasses = null;
    }

    SessionIntegratorFrame frame =
      (SessionIntegratorFrame)getAncestorOfClass(SessionIntegratorFrame.class);
    frame.modelChanged();
  }
  
  private void addListeners() {
    m_bootClassesTableModel.addTableModelListener(this);
    m_bootClassesTable.getSelectionModel().addListSelectionListener(m_bootClassesListener);
    m_addButton.addActionListener(m_addListener);
    m_removeButton.addActionListener(m_removeListener);
  }
  
  private void removeListeners() {
    m_bootClassesTableModel.removeTableModelListener(this);
    m_bootClassesTable.getSelectionModel().removeListSelectionListener(m_bootClassesListener);
    m_addButton.removeActionListener(m_addListener);
    m_removeButton.removeActionListener(m_removeListener);
  }
  
  private void updateChildren() {
    m_bootClassesTableModel.clear();

    if(m_bootClasses != null) {
      String[] bootClasses = m_bootClasses.getIncludeArray();
  
      for(int i = 0; i < bootClasses.length; i++) {
        m_bootClassesTableModel.addBootClass(bootClasses[i]);
      }
    }
  }

  public void setup(DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_dsoApp      = dsoApp;
    m_bootClasses = m_dsoApp != null ?
                    m_dsoApp.getAdditionalBootJarClasses() : null;

    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoApp      = null;
    m_bootClasses = null;

    m_bootClassesTableModel.clear();
    
    setEnabled(false);    
  }
  
  class BootClassTableModel extends DefaultTableModel {
    BootClassTableModel() {
      super();
      setColumnIdentifiers(new String[]{"Boot Classes"});
    }
    
    void clear() {
      setRowCount(0);
    }
    
    void setBootClasses(AdditionalBootJarClasses bootClasses) {
      clear();
      
      if(bootClasses != null) {
        int count = bootClasses.sizeOfIncludeArray();
        
        for(int i = 0; i < count; i++) {
          addBootClass(bootClasses.getIncludeArray(i));
        }
      }
    }
      
    void addBootClass(String typeName) {
      addRow(new Object[] {typeName});
    }
    
    int indexOf(String typeName) {
      int count = getRowCount();
      
      for(int i = 0; i < count; i++) {
        if(((String)getValueAt(i, 0)).equals(typeName)) {
          return i;
        }
      }
      
      return -1;
    }
    
    public void setValueAt(Object value, int row, int col) {
      m_bootClasses.setIncludeArray(row, (String)value);
      super.setValueAt(value, row, col);
    }

    void removeRows(int[] rows) {
      removeTableModelListener(BootClassesPanel.this);
      for(int i = rows.length-1; i >= 0; i--) {
        removeRow(rows[i]);
      }
      addTableModelListener(BootClassesPanel.this);
      syncModel();
    }
  }
  
  public void tableChanged(TableModelEvent tme) {
    syncModel();
  }
  
  private void internalAddBootClass(String typeName) {
    ensureAdditionalBootClasses().addInclude(typeName);
    m_bootClassesTableModel.addBootClass(typeName);
    
    int row = m_bootClassesTableModel.getRowCount()-1;
    m_bootClassesTable.setRowSelectionInterval(row, row);
  }

  private void internalRemoveBootClass(String typeName) {
    int row = m_bootClassesTableModel.indexOf(typeName);
    
    if(row >= 0) {
      ensureAdditionalBootClasses().removeInclude(row);
      m_bootClassesTableModel.removeRow(row);
      
      if(row > 0) {
        row = Math.min(m_bootClassesTableModel.getRowCount()-1, row);
        m_bootClassesTable.setRowSelectionInterval(row, row);
      }
    }
  }
  
  public boolean isBootClass(String typeName) {
    AdditionalBootJarClasses bootClasses = ensureAdditionalBootClasses();
    int                      count       = bootClasses.sizeOfIncludeArray();
    
    for(int i = 0; i < count; i++) {
      if(typeName.equals(bootClasses.getIncludeArray(i))) {
        return true;
      }
    }
    
    return false;
  }
  
  public void ensureBootClass(String typeName) {
    if(!isBootClass(typeName)) {
      internalAddBootClass(typeName);
    }
  }
  
  public void ensureNotBootClass(String fieldName) {
    if(isBootClass(fieldName)) {
      internalRemoveBootClass(fieldName);
    }
  }
}
