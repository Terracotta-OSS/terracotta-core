/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XTable;
import org.terracotta.dso.editors.chooser.FieldChooser;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class RootsPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject              m_project;
  private DsoApplication        m_dsoApp;
  private Roots                 m_roots;
  private XTable                m_rootTable;
  private RootsModel            m_rootsModel;
  private Button                m_addButton;
  private ActionListener        m_addListener;
  private Button                m_removeButton;
  private ActionListener        m_removeListener;
  private ListSelectionListener m_rootsListener;
  private FieldChooser          m_fieldChooser;
  
  public RootsPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_rootTable = (XTable)findComponent("RootTable");
    m_rootTable.setModel(m_rootsModel = new RootsModel());
    m_rootsListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_rootTable.getSelectedRows();
          m_removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };        
    
    m_addButton = (Button)findComponent("AddRootButton");
    m_addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        FieldChooser chsr = getFieldChooser();
        
        chsr.setup(m_project);
        chsr.center(RootsPanel.this.getAncestorOfClass(Frame.class));
        chsr.setVisible(true);
      }
    };

    m_removeButton = (Button)findComponent("RemoveRootButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection = m_rootTable.getSelectedRows();
        int   row;
        
        for(int i = selection.length-1; i >= 0; i--) {
          row = selection[i];
          
          ensureRoots().removeRoot(row);
          m_rootsModel.remove(row);
          m_rootsModel.fireTableRowsDeleted(row, row);
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
           !isRoot(field))
        {
          internalAddRoot(fields[i]);
        }
      }
    }
  }
  
  public boolean hasAnySet() {
    return m_roots != null &&
           m_roots.sizeOfRootArray() > 0;
  }
  
  private Roots ensureRoots() {
    if(m_roots == null) {
      ensureXmlObject();
    }
    return m_roots;
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_roots == null) {
      removeListeners();
      m_roots = m_dsoApp.addNewRoots();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoApp.getRoots() != null){
      m_dsoApp.unsetRoots();
      m_roots = null;
      fireXmlObjectStructureChanged(m_dsoApp);
    }
    
    setDirty();
  }

  private void addListeners() {
    m_rootsModel.addTableModelListener(this);
    m_rootTable.getSelectionModel().addListSelectionListener(m_rootsListener);
    m_addButton.addActionListener(m_addListener);
    m_removeButton.addActionListener(m_removeListener);
  }
  
  private void removeListeners() {
    m_rootsModel.removeTableModelListener(this);
    m_rootTable.getSelectionModel().removeListSelectionListener(m_rootsListener);
    m_addButton.removeActionListener(m_addListener);
    m_removeButton.removeActionListener(m_removeListener);
  }
  
  public void updateChildren() {
    m_rootsModel.set((m_roots != null) ? m_roots.getRootArray() : null);
  }

  public void updateModel() {
    removeListeners();
    updateChildren();
    addListeners();
  }
  
  public void setup(IProject project, DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();
    
    m_project = project;
    m_dsoApp  = dsoApp;
    m_roots   = m_dsoApp != null ? m_dsoApp.getRoots() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_project = null;
    m_dsoApp  = null;
    m_roots   = null;

    m_rootsModel.clear();

    setEnabled(false);
  }
  
  private void internalAddRoot(String fieldName) {
    Root root = ensureRoots().addNewRoot();

    root.setFieldName(fieldName);
    m_rootsModel.add(root);

    int row = m_rootsModel.getRowCount()-1;
    m_rootsModel.fireTableRowsInserted(row, row);
    m_rootTable.setRowSelectionInterval(row, row);
  }

  private static final String[] COLUMNS = {"FieldName", "RootName"};
  private static final String[] HEADERS = {"Field",     "Name"};
  
  class RootsModel extends XObjectTableModel {
    RootsModel() {
      super(Root.class, COLUMNS, HEADERS);
    }
    
    int getFieldRow(String fieldName) {
      Root root;
      int  count = getRowCount();
      
      for(int i = 0; i < count; i++) {
        root = (Root)getObjectAt(i);
        if(root.getFieldName().equals(fieldName)) {
          return i;
        }
      }

      return -1;
    }
  }
  
  public boolean isRoot(String fieldName) {
    Roots roots = ensureRoots();
    int   count = roots.sizeOfRootArray();
    
    for(int i = 0; i < count; i++) {
      if(fieldName.equals(roots.getRootArray(i).getFieldName())) {
        return true;
      }
    }
    
    return false;
  }
  
  public void tableChanged(TableModelEvent e) {
    syncModel();
  }
}
