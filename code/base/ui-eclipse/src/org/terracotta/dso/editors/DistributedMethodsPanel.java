/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IMethod;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.MethodChooser;

import com.tc.admin.common.XTable;
import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.DistributedMethods.MethodExpression;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class DistributedMethodsPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject              m_project;
  private DsoApplication        m_dsoApp;
  private DistributedMethods    m_distributedMethods;
  private XTable                m_methodTable;
  private MethodModel           m_methodModel;
  private Button                m_addButton;
  private ActionListener        m_addListener;
  private Button                m_removeButton;
  private ActionListener        m_removeListener;
  private ListSelectionListener m_methodsListener;
  private MethodChooser         m_methodChooser;  
  
  public DistributedMethodsPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_methodTable = (XTable)findComponent("DistributedMethodTable");
    m_methodTable.setModel(m_methodModel = new MethodModel());
    m_methodsListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int[] sel = m_methodTable.getSelectedRows();
          m_removeButton.setEnabled(sel != null && sel.length > 0);
        }
      }
    };        
    
    m_addButton = (Button)findComponent("AddDistributedMethodButton");
    m_addListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        MethodChooser chsr = getMethodChooser();
        
        chsr.setup(m_project);
        chsr.center(DistributedMethodsPanel.this.getAncestorOfClass(java.awt.Frame.class));
        chsr.setVisible(true);
      }
    };

    m_removeButton = (Button)findComponent("RemoveDistributedMethodButton");
    m_removeListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int[] selection = m_methodTable.getSelectedRows();
        
        for(int i = selection.length-1; i >= 0; i--) {
          ensureDistributedMethods().removeMethodExpression(selection[i]);
          m_methodModel.removeRow(selection[i]);
        }
      }
    };
  }

  private MethodChooser getMethodChooser() {
    if(m_methodChooser == null) {
      Frame owner = (Frame)getAncestorOfClass(Frame.class);
      
      m_methodChooser = new MethodChooser(owner);
      m_methodChooser.setListener(new MethodChooserListener());
    }
    
    return m_methodChooser;
  }

  class MethodChooserListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] exprs = m_methodChooser.getMethodExpressions();
      String   expr;
      
      for(int i = 0; i < exprs.length; i++) {
        expr = exprs[i];
        
        if(expr != null && (expr = expr.trim()) != null && expr.length() > 0) {
          internalAddDistributed(exprs[i]);
        }
      }
    }
  }
  
  public boolean hasAnySet() {
    return m_distributedMethods != null &&
           m_distributedMethods.sizeOfMethodExpressionArray() > 0;
  }
  
  private DistributedMethods ensureDistributedMethods() {
    if(m_distributedMethods == null) {
      ensureXmlObject();
    }
    return m_distributedMethods;
  }
  
  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_distributedMethods == null) {
      removeListeners();
      m_distributedMethods = m_dsoApp.addNewDistributedMethods();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    if(!hasAnySet() && m_dsoApp.getDistributedMethods() != null){
      m_dsoApp.unsetDistributedMethods(); 
      m_distributedMethods = null;      
      fireXmlObjectStructureChanged(m_dsoApp);
    }

    setDirty();
  }
  
  private void addListeners() {
    m_methodModel.addTableModelListener(this);
    m_methodTable.getSelectionModel().addListSelectionListener(m_methodsListener);    
    m_addButton.addActionListener(m_addListener);    
    m_removeButton.addActionListener(m_removeListener);    
  }
  
  private void removeListeners() {
    m_methodModel.removeTableModelListener(this);
    m_methodTable.getSelectionModel().removeListSelectionListener(m_methodsListener);    
    m_addButton.removeActionListener(m_addListener);    
    m_removeButton.removeActionListener(m_removeListener);    
  }
  
  public void updateChildren() {
    m_methodModel.clear();

    if(m_distributedMethods != null) {
      MethodExpression[] mes = m_distributedMethods.getMethodExpressionArray();
      String [] vals = new String[mes.length];
      for (int i = 0; i < mes.length; i++) {
        vals[i] = mes[i].getStringValue();
      }
      m_methodModel.setMethods(vals);
    } else {
      m_methodModel.fireTableDataChanged();
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

    m_project            = project;
    m_dsoApp             = dsoApp;
    m_distributedMethods = m_dsoApp != null ?
                           m_dsoApp.getDistributedMethods() :
                           null;

    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoApp             = null;
    m_distributedMethods = null;

    m_methodModel.clear();
    
    setEnabled(false);
  }
  
  class MethodModel extends DefaultTableModel {
    MethodModel() {
      super();
      setColumnIdentifiers(new String[]{"Distributed methods"});
    }
    
    public int size() {
      return getRowCount();
    }
    
    void clear() {
      setRowCount(0);
    }
    
    void setMethods(String[] methods) {
      clear();
      
      if(methods != null) {
        for(int i = 0; i < methods.length; i++) {
          addMethod(methods[i]);
        }
      }
    }
    
    void addMethod(String method) {
      addRow(new Object[] {method});
    }
    
    String getExcludeAt(int row) {
      return (String)getValueAt(row, 0);
    }
    
    public void setValueAt(Object value, int row, int col) {
      DistributedMethods dms = ensureDistributedMethods();
      MethodExpression me = dms.getMethodExpressionArray(row);
      me.setStringValue((String)value);
      super.setValueAt(value, row, col);
    }
  }

  public void tableChanged(TableModelEvent tme) {
    syncModel();
  }
  
  public boolean isDistributed(IMethod method) {
    return TcPlugin.getDefault().getConfigurationHelper(m_project).isDistributedMethod(method);
  }
  
  private void internalAddDistributed(String expr) {
    if(expr != null && expr.length() > 0) {
      DistributedMethods dms = ensureDistributedMethods();
      MethodExpression me = dms.addNewMethodExpression();
      me.setStringValue(expr);
      m_methodModel.addMethod(expr);
    }
  }
}
