/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.terracotta.ui.session.pattern.PatternHelper;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class InstrumentedClassesPanel extends XContainer implements TableModelListener {
  private DsoApplication        dsoApp;
  private InstrumentedClasses   instrumentedClasses;
  private RuleTable             ruleTable;
  private RuleModel             ruleModel;
  private XButton               addRuleButton;
  private ActionListener        addRuleListener;
  private XButton               removeRuleButton;
  private ActionListener        removeRuleListener;
  private XButton               moveUpButton;
  private ActionListener        moveUpListener;
  private XButton               moveDownButton;
  private ActionListener        moveDownListener;
  private ListSelectionListener rulesListener;

  public InstrumentedClassesPanel() {
    super(new BorderLayout());

    setBorder(BorderFactory.createTitledBorder("Instrumentation Rules"));

    ruleTable = new RuleTable();
    ruleTable.setModel(ruleModel = new RuleModel());
    rulesListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent lse) {
        if (!lse.getValueIsAdjusting()) {
          int sel = ruleTable.getSelectedRow();
          int rowCount = ruleModel.size();

          removeRuleButton.setEnabled(sel != -1);
          moveUpButton.setEnabled(rowCount > 1 && sel > 0);
          moveDownButton.setEnabled(rowCount > 1 && sel < rowCount - 1);
        }
      }
    };
    add(new XScrollPane(ruleTable));

    XContainer rightPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    addRuleButton = new XButton("Add");
    addRuleListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String include = JOptionPane.showInputDialog("Enter rule expression", "");

        if (include != null) {
          include = include.trim();
          if (include != null && include.length() > 0) {
            internalAddInclude(include);
          }
        }
      }
    };
    rightPanel.add(addRuleButton, gbc);
    gbc.gridy++;

    removeRuleButton = new XButton("Remove");
    removeRuleListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int row = ruleTable.getSelectedRow();
        ruleModel.removeRuleAt(row);
        syncModel();
      }
    };
    rightPanel.add(removeRuleButton, gbc);
    gbc.gridy++;

    moveUpButton = new XButton();
    moveUpButton.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/view_menu.gif")));
    moveUpButton.setToolTipText("More general");
    moveUpListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int newRow = ruleTable.moveUp();
        syncModel();
        ruleTable.setRowSelectionInterval(newRow, newRow);
      }
    };
    rightPanel.add(moveUpButton, gbc);
    gbc.gridy++;

    moveDownButton = new XButton();
    moveDownButton.setIcon(new ImageIcon(getClass().getResource("/com/tc/admin/icons/hide_menu.gif")));
    moveDownButton.setToolTipText("More specific");
    moveDownListener = new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        int newRow = ruleTable.moveDown();
        syncModel();
        ruleTable.setRowSelectionInterval(newRow, newRow);

      }
    };
    rightPanel.add(moveDownButton, gbc);

    add(rightPanel, BorderLayout.EAST);
  }

  public boolean hasAnySet() {
    return instrumentedClasses != null
           && (instrumentedClasses.sizeOfExcludeArray() > 0 || instrumentedClasses.sizeOfIncludeArray() > 0);
  }

  private InstrumentedClasses ensureInstrumentedClasses() {
    if (instrumentedClasses == null) {
      ensureXmlObject();
    }
    return instrumentedClasses;
  }

  public void ensureXmlObject() {
    if (instrumentedClasses == null) {
      removeListeners();
      instrumentedClasses = dsoApp.addNewInstrumentedClasses();
      updateChildren();
      addListeners();
    }
  }

  private void syncModel() {
    SessionIntegratorFrame frame = (SessionIntegratorFrame) SwingUtilities
        .getAncestorOfClass(SessionIntegratorFrame.class, this);
    if (frame != null) {
      frame.modelChanged();
    }
  }

  private void addListeners() {
    ruleModel.addTableModelListener(this);
    ruleTable.getSelectionModel().addListSelectionListener(rulesListener);
    addRuleButton.addActionListener(addRuleListener);
    removeRuleButton.addActionListener(removeRuleListener);
    moveUpButton.addActionListener(moveUpListener);
    moveDownButton.addActionListener(moveDownListener);
  }

  private void removeListeners() {
    ruleModel.removeTableModelListener(this);
    ruleTable.getSelectionModel().removeListSelectionListener(rulesListener);
    addRuleButton.removeActionListener(addRuleListener);
    removeRuleButton.removeActionListener(removeRuleListener);
    moveUpButton.removeActionListener(moveUpListener);
    moveDownButton.removeActionListener(moveDownListener);
  }

  private void updateChildren() {
    ruleModel.setInstrumentedClasses(instrumentedClasses);
  }

  public void setup(DsoApplication dsoApp) {
    setEnabled(true);
    moveUpButton.setEnabled(false);
    moveDownButton.setEnabled(false);
    removeListeners();

    this.dsoApp = dsoApp;
    instrumentedClasses = dsoApp != null ? dsoApp.getInstrumentedClasses() : null;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    dsoApp = null;
    instrumentedClasses = null;

    ruleModel.clear();

    setEnabled(false);
  }

  private boolean isAdaptable(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int size = classes.sizeOfIncludeArray();
    Include include;
    String expr;

    for (int i = 0; i < size; i++) {
      include = classes.getIncludeArray(i);
      expr = include.getClassExpression();

      if (PatternHelper.getHelper().matchesClass(expr, classExpr)) { return true; }
    }

    return false;
  }

  private void internalAddInclude(String classExpr) {
    Include include = instrumentedClasses.addNewInclude();
    include.setClassExpression(classExpr);
    syncModel();

    // int row = ruleModel.getRowCount();
    // ruleModel.addInclude(classExpr);
    // ruleModel.fireTableRowsInserted(row, row);
  }

  private void internalRemoveInclude(String classExpr) {
    int count = ruleModel.getRowCount();
    Rule rule;
    String expr;

    for (int i = 0; i < count; i++) {
      rule = ruleModel.getRuleAt(i);

      if (rule.isIncludeRule()) {
        expr = ((Include) rule).getClassExpression();

        if (PatternHelper.getHelper().matchesClass(expr, classExpr)) {
          ruleModel.removeRuleAt(i);
          syncModel();
          // ruleModel.fireTableRowsDeleted(i, i);
          break;
        }
      }
    }
  }

  private boolean isExcluded(String classExpr) {
    InstrumentedClasses classes = ensureInstrumentedClasses();
    int size = classes.sizeOfExcludeArray();
    String expr;

    for (int i = 0; i < size; i++) {
      expr = classes.getExcludeArray(i);
      if (PatternHelper.getHelper().matchesClass(expr, classExpr)) { return true; }
    }

    return false;
  }

  private void internalAddExclude(String expr) {
    if (expr != null && expr.length() > 0) {
      ClassExpression classExpr = instrumentedClasses.addNewExclude();
      classExpr.setStringValue(expr);
      syncModel();
      // ruleModel.addExclude(expr);
    }
  }

  private void internalRemoveExclude(String classExpr) {
    int count = ruleModel.size();
    Rule rule;

    for (int i = 0; i < count; i++) {
      rule = ruleModel.getRuleAt(i);

      if (rule.isExcludeRule()) {
        if (PatternHelper.getHelper().matchesClass(rule.getExpression(), classExpr)) {
          ruleModel.removeRuleAt(i);
          syncModel();
          // ruleModel.fireTableRowsDeleted(i, i);
          break;
        }
      }
    }
  }

  public void ensureAdaptable(String classExpr) {
    internalAddInclude(classExpr);
  }

  public void ensureNotAdaptable(String classExpr) {
    if (isAdaptable(classExpr)) {
      internalRemoveInclude(classExpr);
    }
  }

  public void ensureExcluded(String classExpr) {
    if (!isExcluded(classExpr)) {
      internalAddExclude(classExpr);
    }
  }

  public void ensureNotExcluded(String classExpr) {
    if (isExcluded(classExpr)) {
      internalRemoveExclude(classExpr);
    }
  }

  public void tableChanged(TableModelEvent e) {
    syncModel();
  }
}
