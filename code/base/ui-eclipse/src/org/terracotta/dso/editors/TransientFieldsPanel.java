/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ContainerResource;

import org.terracotta.dso.TcPlugin;
import com.tc.admin.common.XTable;
import org.terracotta.dso.editors.chooser.FieldChooser;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TransientFields;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class TransientFieldsPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject              m_project;
  private DsoApplication        m_dsoApp;
  private TransientFields       m_transientFields;
  private XTable                m_transientTable;
  private TransientTableModel   m_transientTableModel;
  private Button                m_addButton;
  private ActionListener        m_addListener;
  private Button                m_removeButton;
  private ActionListener        m_removeListener;
  private ListSelectionListener m_transientsListener;
  private FieldChooser          m_fieldChooser;
  
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
        FieldChooser chsr = getFieldChooser();
        
        chsr.setup(m_project);
        chsr.center(TransientFieldsPanel.this.getAncestorOfClass(java.awt.Frame.class));
        chsr.setVisible(true);
      }
    };

    m_removeButton = (Button)findComponent("RemoveTransientButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection  = m_transientTable.getSelectedRows();
        
        for(int i = selection.length-1; i >= 0; i--) {
          ensureTransientFields().removeFieldName(selection[i]);
          m_transientTableModel.removeRow(selection[i]);
        }
      }
    };
  }

  private FieldChooser getFieldChooser() {
    if(m_fieldChooser == null) {
      m_fieldChooser = new FieldChooser((Frame)getAncestorOfClass(Frame.class));
      m_fieldChooser.setListener(new FieldChooserListener());
    }
    
    return m_fieldChooser;
  }

  class FieldChooserListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] fields = m_fieldChooser.getFieldNames();
      String   field;
      
      for(int i = 0; i < fields.length; i++) {
        field = fields[i];
        
        if(field != null &&
           (field = field.trim()) != null &&
            field.length() > 0 &&
           !isTransient(fields[i]))
        {
          internalAddTransient(fields[i]);
        }
      }
    }
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
    super.ensureXmlObject();

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
      fireXmlObjectStructureChanged(m_dsoApp);
    }

    setDirty();
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
  
  public void updateChildren() {
    m_transientTableModel.clear();

    if(m_transientFields != null) {
      String[] transients = m_transientFields.getFieldNameArray();
  
      for(int i = 0; i < transients.length; i++) {
        m_transientTableModel.addField(transients[i]);
      }
    }
  }

  public void updateModel() {
    removeListeners();
    updateChildren();
    addListeners();
  }
  
  public void setup(IProject project, DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_project         = project;
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

  public boolean isTransient(String fieldName) {
    return TcPlugin.getDefault().getConfigurationHelper(m_project).isTransient(fieldName);
  }
}
