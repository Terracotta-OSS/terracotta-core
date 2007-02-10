/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XTable;
import com.terracottatech.configV2.AdditionalBootJarClasses;
import com.terracottatech.configV2.DsoApplication;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class BootClassesPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject                 m_project;
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
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            IJavaProject     javaProject = JavaCore.create(m_project);
            int              filter      = IJavaSearchScope.SYSTEM_LIBRARIES;
            IJavaElement[]   elements    = new IJavaElement[]{javaProject};
            IJavaSearchScope scope       = SearchEngine.createJavaSearchScope(elements, filter);
            int              style       = IJavaElementSearchConstants.CONSIDER_ALL_TYPES;
            SelectionDialog  dialog;
            
            try {
              dialog = JavaUI.createTypeDialog(null, null, scope, style, true);
            } catch(JavaModelException jme) {
              jme.printStackTrace();
              return;
            }
              
            dialog.setTitle("DSO Application Configuration");
            dialog.setMessage("Select system classes to add to DSO Boot Jar");
            dialog.open();
            
            final Object[] result = dialog.getResult();
            
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                IType type;
            
                if(result != null) {
                  for(int i = 0; i < result.length; i++) {
                    type = (IType)result[i];
                    internalAddBootClass(type.getFullyQualifiedName());
                  }
                }
              }
            });
          }
        });
      }
    };

    m_removeButton = (Button)findComponent("RemoveBootClassButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection  = m_bootClassesTable.getSelectedRows();
        
        for(int i = selection.length-1; i >= 0; i--) {
          ensureAdditionalBootClasses().removeInclude(selection[i]);
          m_bootClassesTableModel.removeRow(selection[i]);
        }
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
    super.ensureXmlObject();

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
      fireXmlObjectStructureChanged(m_dsoApp);
    }

    setDirty();
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
  
  public void updateChildren() {
    m_bootClassesTableModel.clear();

    if(m_bootClasses != null) {
      String[] bootClasses = m_bootClasses.getIncludeArray();
  
      for(int i = 0; i < bootClasses.length; i++) {
        m_bootClassesTableModel.addBootClass(bootClasses[i]);
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

    m_project     = project;
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
}
