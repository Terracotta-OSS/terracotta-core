/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ContainerResource;

import org.terracotta.dso.editors.chooser.ClassChooser;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.InstrumentedClasses;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class InstrumentedClassesPanel extends ConfigurationEditorPanel
  implements TableModelListener
{
  private IProject              m_project;
  private DsoApplication        m_dsoApp;
  private InstrumentedClasses   m_instrumentedClasses;
  private RuleTable             m_ruleTable;
  private RuleModel             m_ruleModel;
  private Button                m_addRuleButton;
  private ActionListener        m_addRuleListener;
  private Button                m_removeRuleButton;
  private ActionListener        m_removeRuleListener;
  private Button                m_moveUpButton;
  private ActionListener        m_moveUpListener;
  private Button                m_moveDownButton;
  private ActionListener        m_moveDownListener;
  private ListSelectionListener m_rulesListener;
  private ClassChooser          m_classChooser;
  private ActionListener        m_instrumentedClassChooserListener;  
  
  public InstrumentedClassesPanel() {
    super();
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_ruleTable = (RuleTable)findComponent("RuleTable");
    m_ruleTable.setModel(m_ruleModel = new RuleModel());
    m_rulesListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if(!lse.getValueIsAdjusting()) {
          int sel      = m_ruleTable.getSelectedRow();
          int rowCount = m_ruleModel.size();
          
          m_removeRuleButton.setEnabled(sel != -1);
          m_moveUpButton.setEnabled(rowCount > 1 && sel > 0);
          m_moveDownButton.setEnabled(rowCount > 1 && sel < rowCount-1);
        }
      }
    };        
    
    m_addRuleButton = (Button)findComponent("AddRuleButton");
    m_addRuleListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        ClassChooser chsr = getInstrumentedClassChooser();
        
        chsr.setup(m_project);
        chsr.center(InstrumentedClassesPanel.this.getAncestorOfClass(Frame.class));
        chsr.setVisible(true);
      }
    };

    m_removeRuleButton = (Button)findComponent("RemoveRuleButton");
    m_removeRuleListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int row = m_ruleTable.getSelectedRow();
        
        m_ruleModel.removeRuleAt(row);
        m_ruleModel.fireTableDataChanged();
      }
    };
    
    m_moveUpButton = (Button)findComponent("MoveUpButton");
    m_moveUpListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_ruleTable.moveUp();
      }
    };

    m_moveDownButton = (Button)findComponent("MoveDownButton");
    m_moveDownListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_ruleTable.moveDown(); 
      }
    };
  }
  
  private ActionListener getInstrumentedClassChooserListener() {
    if(m_instrumentedClassChooserListener == null) {
      m_instrumentedClassChooserListener = new InstrumentedClassChooserListener();
    }
    
    return m_instrumentedClassChooserListener;
  }
  
  private ClassChooser getInstrumentedClassChooser() {
    if(m_classChooser == null) {
      m_classChooser = new ClassChooser((Frame)getAncestorOfClass(Frame.class));
    }
    m_classChooser.setListener(getInstrumentedClassChooserListener());
    
    return m_classChooser;
  }

  class InstrumentedClassChooserListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      String[] classExprs = m_classChooser.getClassnames();
      String   classExpr;
      
      for(int i = 0; i < classExprs.length; i++) {
        classExpr = classExprs[i];
        
        if(classExpr != null &&
           (classExpr = classExpr.trim()) != null &&
            classExpr.length() > 0)
        {
          internalAddInclude(classExpr);
        }
      }
    }
  }

  public boolean hasAnySet() {
    return m_instrumentedClasses != null &&
          (m_instrumentedClasses.sizeOfExcludeArray() > 0 ||
           m_instrumentedClasses.sizeOfIncludeArray() > 0);
  }
 
  public void ensureXmlObject() {
    super.ensureXmlObject();
    
    if(m_instrumentedClasses == null) {
      removeListeners();
      m_instrumentedClasses = m_dsoApp.addNewInstrumentedClasses();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    setDirty();
  }
  
  private void addListeners() {
    m_ruleModel.addTableModelListener(this);
    m_ruleTable.getSelectionModel().addListSelectionListener(m_rulesListener);
    m_addRuleButton.addActionListener(m_addRuleListener);
    m_removeRuleButton.addActionListener(m_removeRuleListener);
    m_moveUpButton.addActionListener(m_moveUpListener);
    m_moveDownButton.addActionListener(m_moveDownListener);
  }
  
  private void removeListeners() {
    m_ruleModel.removeTableModelListener(this);
    m_ruleTable.getSelectionModel().removeListSelectionListener(m_rulesListener);
    m_addRuleButton.removeActionListener(m_addRuleListener);
    m_removeRuleButton.removeActionListener(m_removeRuleListener);
    m_moveUpButton.removeActionListener(m_moveUpListener);
    m_moveDownButton.removeActionListener(m_moveDownListener);
  }
  
  public void updateChildren() {
    m_ruleModel.setInstrumentedClasses(m_instrumentedClasses);
  }
  
  public void updateModel() {
    removeListeners();
    updateChildren();
    addListeners();
  }
  
  public void setup(IProject project, DsoApplication dsoApp) {
    setEnabled(true);
    removeListeners();

    m_project             = project;
    m_dsoApp              = dsoApp;
    m_instrumentedClasses = m_dsoApp != null ?
                            m_dsoApp.getInstrumentedClasses() :
                            null;

    updateChildren();                        
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoApp              = null;
    m_instrumentedClasses = null;
    
    m_ruleModel.clear();
    
    setEnabled(false);
  }
  
  private void internalAddInclude(String classExpr) {
    int row = m_ruleModel.getRowCount();
    
    m_ruleModel.addInclude(classExpr);
    m_ruleModel.fireTableRowsInserted(row, row);
  }
  
  public void tableChanged(TableModelEvent e) {
    syncModel();
  }
}
