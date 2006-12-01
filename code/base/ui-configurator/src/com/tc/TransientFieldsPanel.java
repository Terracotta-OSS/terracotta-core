/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTable;
import com.terracottatech.configV2.DsoApplication;
import com.terracottatech.configV2.TransientFields;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class TransientFieldsPanel extends XContainer
  implements TableModelListener
{
  private DsoApplication        m_dsoApp;
  private TransientFields       m_transientFields;
  private XTable                m_transientTable;
  private TransientTableModel   m_transientTableModel;
  private Button                m_addButton;
  private ActionListener        m_addListener;
  private Button                m_removeButton;
  private ActionListener        m_removeListener;
  private ListSelectionListener m_transientsListener;
  
  public TransientFieldsPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_transientTable = (XTable)findComponent("TransientFieldTable");
    m_transientTable.setModel(m_transientTableModel = new TransientTableModel());
    m_transientsListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_transientTable.getSelectedRows();
          m_removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };        
    
    m_addButton = (Button)findComponent("AddTransientButton");
    m_addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String field = JOptionPane.showInputDialog("Transient field", "");
        
        if(field != null) {
          field = field.trim();
          
          if(field != null && field.length() > 0 && !isTransient(field)) {
            internalAddTransient(field);
          }
        }
      }
    };

    m_removeButton = (Button)findComponent("RemoveTransientButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        TransientFields tf        = ensureTransientFields();
        int[]           selection = m_transientTable.getSelectedRows();
        
        for(int i = selection.length-1; i >= 0; i--) {
          tf.removeFieldName(selection[i]);
        }
        m_transientTableModel.removeRows(selection);
      }
    };
  }

  public boolean hasAnySet() {
    return m_transientFields != null &&
           m_transientFields.sizeOfFieldNameArray() > 0;
  }
  
  private TransientFields ensureTransientFields() {
    if(m_transientFields == null) {
      ensureXmlObject();
    }
    return m_transientFields;
  }
  
  public void ensureXmlObject() {
    if(m_transientFields == null) {
      removeListeners();
      m_transientFields = m_dsoApp.addNewTransientFields();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoApp.getTransientFields() != null){
      m_dsoApp.unsetTransientFields();
      m_transientFields = null;
    }

    SessionIntegratorFrame frame =
      (SessionIntegratorFrame)getAncestorOfClass(SessionIntegratorFrame.class);
    frame.modelChanged();
  }
  
  private void addListeners() {
    m_transientTableModel.addTableModelListener(this);
    m_transientTable.getSelectionModel().addListSelectionListener(m_transientsListener);
    m_addButton.addActionListener(m_addListener);
    m_removeButton.addActionListener(m_removeListener);
  }
  
  private void removeListeners() {
    m_transientTableModel.removeTableModelListener(this);
    m_transientTable.getSelectionModel().removeListSelectionListener(m_transientsListener);
    m_addButton.removeActionListener(m_addListener);
    m_removeButton.removeActionListener(m_removeListener);
  }
  
  private void updateChildren() {
    m_transientTableModel.clear();

    if(m_transientFields != null) {
      String[] transients = m_transientFields.getFieldNameArray();
  
      for(int i = 0; i < transients.length; i++) {
        m_transientTableModel.addField(transients[i]);
      }
    }
  }

  public void setup(DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_dsoApp          = dsoApp;
    m_transientFields = m_dsoApp != null ?
                        m_dsoApp.getTransientFields() :
                        null;

    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoApp          = null;
    m_transientFields = null;

    m_transientTableModel.clear();
    
    setEnabled(false);    
  }
  
  class TransientTableModel extends DefaultTableModel {
    TransientTableModel() {
      super();
      setColumnIdentifiers(new String[]{"Transient fields"});
    }
    
    void clear() {
      setRowCount(0);
    }
    
    void setTransientFields(TransientFields fields) {
      clear();
      
      if(fields != null) {
        int count = fields.sizeOfFieldNameArray();
        
        for(int i = 0; i < count; i++) {
          addField(fields.getFieldNameArray(i));
        }
      }
    }
      
    void addField(String fieldName) {
      addRow(new Object[] {fieldName});
    }
    
    int indexOf(String fieldName) {
      int count = getRowCount();
      
      for(int i = 0; i < count; i++) {
        if(((String)getValueAt(i, 0)).equals(fieldName)) {
          return i;
        }
      }
      
      return -1;
    }
    
    public void setValueAt(Object value, int row, int col) {
      m_transientFields.setFieldNameArray(row, (String)value);
      super.setValueAt(value, row, col);
    }

    void removeRows(int[] rows) {
      removeTableModelListener(TransientFieldsPanel.this);
      for(int i = rows.length-1; i >= 0; i--) {
        removeRow(rows[i]);
      }
      addTableModelListener(TransientFieldsPanel.this);
      syncModel();
    }
  }
  
  public void tableChanged(TableModelEvent tme) {
    syncModel();
  }
  
  private void internalAddTransient(String fieldName) {
    ensureTransientFields().addFieldName(fieldName);
    m_transientTableModel.addField(fieldName);
    
    int row = m_transientTableModel.getRowCount()-1;
    m_transientTable.setRowSelectionInterval(row, row);
  }

  private void internalRemoveTransient(String fieldName) {
    int row = m_transientTableModel.indexOf(fieldName);
    
    if(row >= 0) {
      ensureTransientFields().removeFieldName(row);
      m_transientTableModel.removeRow(row);
      
      if(row > 0) {
        row = Math.min(m_transientTableModel.getRowCount()-1, row);
        m_transientTable.setRowSelectionInterval(row, row);
      }
    }
  }
  
  public boolean isTransient(String fieldName) {
    TransientFields transients = ensureTransientFields();
    int             count      = transients.sizeOfFieldNameArray();
    
    for(int i = 0; i < count; i++) {
      if(fieldName.equals(transients.getFieldNameArray(i))) {
        return true;
      }
    }
    
    return false;
  }
  
  public void ensureTransient(String fieldName) {
    if(!isTransient(fieldName)) {
      internalAddTransient(fieldName);
    }
  }
  
  public void ensureNotTransient(String fieldName) {
    if(isTransient(fieldName)) {
      internalRemoveTransient(fieldName);
    }
  }
}
