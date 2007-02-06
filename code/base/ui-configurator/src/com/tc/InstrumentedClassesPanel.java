/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.Button;
import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.pattern.PatternHelper;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class InstrumentedClassesPanel extends XContainer
  implements TableModelListener
{
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
  private boolean               m_syncingModel;
  
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
        String include = JOptionPane.showInputDialog("Enter rule expression", "");
        
        if(include != null) {
          include = include.trim();
          
          if(include != null && include.length() > 0) {
            internalAddInclude(include);
          }
        }
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
  
  public boolean hasAnySet() {
    return m_instrumentedClasses != null &&
          (m_instrumentedClasses.sizeOfExcludeArray() > 0 ||
           m_instrumentedClasses.sizeOfIncludeArray() > 0);
  }
 
  private InstrumentedClasses ensureInstrumentedClasses() {
    if(m_instrumentedClasses == null) {
      ensureXmlObject();
    }
    return m_instrumentedClasses;
  }
  
  public void ensureXmlObject() {
    if(m_instrumentedClasses == null) {
      removeListeners();
      m_instrumentedClasses = m_dsoApp.addNewInstrumentedClasses();
      updateChildren();
      addListeners();
    }
  }
  
  private void syncModel() {
    m_syncingModel = true;
    SessionIntegratorFrame frame =
      (SessionIntegratorFrame)getAncestorOfClass(SessionIntegratorFrame.class);
    frame.modelChanged();
    m_syncingModel = false;    
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
  
  private void updateChildren() {
    m_ruleModel.setInstrumentedClasses(m_instrumentedClasses);
  }
  
  public void setup(DsoApplication dsoApp) {
    if(!m_syncingModel) {
      setEnabled(true);
      m_moveUpButton.setEnabled(false);
      m_moveDownButton.setEnabled(false);
      removeListeners();
  
      m_dsoApp              = dsoApp;
      m_instrumentedClasses = m_dsoApp != null ?
                              m_dsoApp.getInstrumentedClasses() :
                              null;
  
      updateChildren();                        
      addListeners();
    }
  }
  
  public void tearDown() {
    removeListeners();
    
    m_dsoApp              = null;
    m_instrumentedClasses = null;
    
    m_ruleModel.clear();
    
    setEnabled(false);
  }
  
  private boolean isAdaptable(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int                 size    = classes.sizeOfIncludeArray();
    Include             include;
    String              expr;
    
    for(int i = 0; i < size; i++) {
      include = classes.getIncludeArray(i);
      expr    = include.getClassExpression();

      if(PatternHelper.getHelper().matchesClass(expr, classExpr)) {
        return true;
      }
    }
    
    return false;
  }
  
  private void internalAddInclude(String classExpr) {
    int row = m_ruleModel.getRowCount();
    
    m_ruleModel.addInclude(classExpr);
    m_ruleModel.fireTableRowsInserted(row, row);
  }
  
  private void internalRemoveInclude(String classExpr) {
    int     count = m_ruleModel.getRowCount();
    Rule    rule;
    String  expr;
    
    for(int i = 0; i < count; i++) {
      rule = m_ruleModel.getRuleAt(i);

      if(rule.isIncludeRule()) {
        expr = ((Include)rule).getClassExpression();

        if(PatternHelper.getHelper().matchesClass(expr, classExpr)) {
          m_ruleModel.removeRuleAt(i);
          m_ruleModel.fireTableRowsDeleted(i, i);
          break;
        }
      }
    }
  }
  
  private boolean isExcluded(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int                 size    = classes.sizeOfExcludeArray();
    String              expr;
    
    for(int i = 0; i < size; i++) {
      expr = classes.getExcludeArray(i);
      
      if(PatternHelper.getHelper().matchesClass(expr, classExpr)) {
        return true;
      }
    }
    
    return false;
  }
  
  private void internalAddExclude(String classExpr) {
    if(classExpr != null && classExpr.length() > 0) {
      m_ruleModel.addExclude(classExpr);
    }
  }
  
  private void internalRemoveExclude(String classExpr) {
    int  count = m_ruleModel.size();
    Rule rule;
    
    for(int i = 0; i < count; i++) {
      rule = m_ruleModel.getRuleAt(i);
      
      if(rule.isExcludeRule()) {
        if(PatternHelper.getHelper().matchesClass(rule.getExpression(), classExpr)) {
          m_ruleModel.removeRuleAt(i);
          m_ruleModel.fireTableRowsDeleted(i, i);
          break;
        }
      }
    }
  }

  public void ensureAdaptable(String classExpr) {
    internalAddInclude(classExpr);
  }
  
  public void ensureNotAdaptable(String classExpr) {
    if(isAdaptable(classExpr)) {
      internalRemoveInclude(classExpr);
    }
  }
  
  public void ensureExcluded(String classExpr) {
    if(!isExcluded(classExpr)) {
      internalAddExclude(classExpr);
    }
  }
  
  public void ensureNotExcluded(String classExpr) {
    if(isExcluded(classExpr)) {
      internalRemoveExclude(classExpr);
    }
  }

  public void tableChanged(TableModelEvent e) {
    syncModel();
  }
}
