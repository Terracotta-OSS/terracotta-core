/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.commons.lang.StringUtils;

import com.tc.admin.common.PagedView;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XTableCellRenderer;
import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

public class RuleTable extends XObjectTable {
  private OnLoadDialog           onLoadDialog;

  private static final String    MOVE_UP_ACTION   = "MoveUp";
  private static final String    MOVE_DOWN_ACTION = "MoveDown";

  private static final KeyStroke MOVE_UP_STROKE   = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK);
  private static final KeyStroke MOVE_DOWN_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK);

  public RuleTable() {
    super();

    setDefaultRenderer(Integer.class, new RuleTypeRenderer());
    setDefaultEditor(Integer.class, new RuleTypeEditor());

    setDefaultRenderer(RuleDetail.class, new RuleDetailRenderer());
    setDefaultEditor(RuleDetail.class, new RuleDetailEditor(true));

    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ActionMap actionMap = getActionMap();
    actionMap.put(MOVE_UP_ACTION, new MoveUpAction());
    actionMap.put(MOVE_DOWN_ACTION, new MoveDownAction());

    InputMap inputMap = getInputMap();
    inputMap.put(MOVE_UP_STROKE, MOVE_UP_ACTION);
    inputMap.put(MOVE_DOWN_STROKE, MOVE_DOWN_ACTION);
  }

  public OnLoadDialog getOnLoadDialog() {
    if (onLoadDialog == null) {
      SessionIntegratorFrame frame = (SessionIntegratorFrame) SwingUtilities
          .getAncestorOfClass(SessionIntegratorFrame.class, this);
      onLoadDialog = new OnLoadDialog(frame);
    }
    return onLoadDialog;
  }

  class RuleTypeRenderer extends XTableCellRenderer {
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int col) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
      Rule rule = getRuleAt(row);
      setText(rule.isExcludeRule() ? "Exclude" : "Include");
      return this;
    }
  }

  class RuleTypeEditor extends XCellEditor {
    private static final String  INCLUDE_ITEM = "Include";
    private static final String  EXCLUDE_ITEM = "Exclude";

    private final String[]       MODEL_ITEMS  = new String[] { INCLUDE_ITEM, EXCLUDE_ITEM };

    private int                  editorRow;
    private DefaultComboBoxModel model;

    RuleTypeEditor() {
      super(new XComboBox());
      model = new DefaultComboBoxModel(MODEL_ITEMS);
      ((XComboBox) editorComponent).setModel(model);
    }

    protected void fireEditingStopped() {
      super.fireEditingStopped();
      setRowSelectionInterval(editorRow, editorRow);
    }

    public Object getCellEditorValue() {
      Object value = super.getCellEditorValue();
      return new Integer(value.equals(INCLUDE_ITEM) ? Rule.INCLUDE_RULE : Rule.EXCLUDE_RULE);
    }

    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                          int col) {
      super.getTableCellEditorComponent(table, value, isSelected, row, col);
      Rule rule = getRuleAt(row);
      model.setSelectedItem(MODEL_ITEMS[rule.getType()]);
      this.editorRow = row;
      return editorComponent;
    }
  }

  class RuleDetailRenderer extends XTableCellRenderer {
    protected TableCellEditor editor;

    public RuleDetailRenderer() {
      super();
      editor = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new RuleDetailEditor(false);
    }

    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int col) {
      JComponent c = (JComponent) editor.getTableCellEditorComponent(table, value, isSelected, row, col);
      c.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : null);
      return c;
    }
  }

  private static String buildToolTip(Include include) {
    OnLoad onLoad = include.getOnLoad();
    String tip = null;

    if (onLoad != null) {
      if ((tip = onLoad.getMethod()) == null) {
        if ((tip = onLoad.getExecute()) != null) {
          StringBuffer sb = new StringBuffer();
          String[] lines = StringUtils.split(onLoad.getExecute(), System.getProperty("line.separator"));

          sb.append("<html>");
          for (int i = 0; i < lines.length; i++) {
            sb.append("<p>");
            sb.append(lines[i]);
          }
          sb.append("</html>");

          tip = sb.toString();
        }
      }
    }

    return tip;
  }

  class OnLoadButton extends XButton {
    int row, col;

    OnLoadButton(String text) {
      super(text);
    }

    void setCell(int row, int col) {
      this.row = row;
      this.col = col;
    }

    int getRow() {
      return row;
    }

    int getCol() {
      return col;
    }
  }

  class RuleDetailEditor extends XCellEditor {
    PagedView       pagedView;
    RuleDetailPanel detailPanel;
    OnLoadButton    onLoadButton;
    XCheckBox       honorTransientToggle;
    XLabel          excludeRenderer;
    boolean         alwaysSelected;

    RuleDetailEditor(boolean alwaysSelected) {
      super(new XCheckBox("Honor transient"));

      this.alwaysSelected = alwaysSelected;

      honorTransientToggle = (XCheckBox) editorComponent;
      honorTransientToggle.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          boolean honor = honorTransientToggle.isSelected();
          int row = onLoadButton.getRow();
          int col = onLoadButton.getCol();
          getIncludeRuleAt(row).setHonorTransient(honor);
          getRuleModel().fireTableCellUpdated(row, col);
          setRowSelectionInterval(row, row);
        }
      });
      honorTransientToggle.setMargin(new Insets(0, 0, 0, 0));

      onLoadButton = new OnLoadButton("On load...");
      onLoadButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          int row = onLoadButton.getRow();
          getOnLoadDialog().edit(getIncludeAt(row));
          stopCellEditing();
          setRowSelectionInterval(row, row);
        }
      });
      pagedView = new PagedView();

      detailPanel = new RuleDetailPanel(honorTransientToggle, onLoadButton);
      detailPanel.setName("Include");
      pagedView.addPage(detailPanel);

      excludeRenderer = new XLabel();
      excludeRenderer.setName("Exclude");
      pagedView.addPage(excludeRenderer);

      editorComponent = pagedView;

      clickCountToStart = 1;
    }

    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                          int col) {
      Rule rule = getRuleAt(row);

      if (alwaysSelected) {
        isSelected = true;
      }

      Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
      Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();

      if (rule.isIncludeRule()) {
        Include include = getIncludeAt(row);

        onLoadButton.setCell(row, col);
        onLoadButton.setForeground(table.getForeground());
        onLoadButton.setBackground(table.getBackground());
        onLoadButton.setFont(table.getFont());
        onLoadButton.setToolTipText(buildToolTip(include));

        honorTransientToggle.setSelected(include.getHonorTransient());
        honorTransientToggle.setForeground(fg);
        honorTransientToggle.setBackground(bg);
        honorTransientToggle.setFont(table.getFont());
        honorTransientToggle.setOpaque(true);

        detailPanel.setForeground(fg);
        detailPanel.setBackground(bg);

        editorComponent.setForeground(fg);
        editorComponent.setBackground(bg);
        editorComponent.setOpaque(true);

        pagedView.setPage("Include");
      } else {
        excludeRenderer.setForeground(fg);
        excludeRenderer.setBackground(bg);
        excludeRenderer.setOpaque(true);
        pagedView.setPage("Exclude");
      }

      return editorComponent;
    }

    public boolean stopCellEditing() {
      removeEditor();
      return true;
    }
  }

  public RuleModel getRuleModel() {
    return (RuleModel) getModel();
  }

  public Include getIncludeAt(int row) {
    return getIncludeRuleAt(row).getInclude();
  }

  public IncludeRule getIncludeRuleAt(int row) {
    return (IncludeRule) getRuleAt(row);
  }

  public Rule getRuleAt(int row) {
    return getRuleModel().getRuleAt(row);
  }

  public int moveUp() {
    int row = getSelectedRow();
    if (isEditing()) {
      removeEditor();
    }
    if (row > 0) {
      getRuleModel().moveRuleUp(row--);
    }
    return row;
  }

  class MoveUpAction extends XAbstractAction {
    MoveUpAction() {
      super("Move up");
    }

    public void actionPerformed(ActionEvent ae) {
      moveUp();
    }
  }

  public int moveDown() {
    int row = getSelectedRow();
    if (isEditing()) {
      removeEditor();
    }
    if (row != -1 && row < getRuleModel().getRowCount() - 1) {
      getRuleModel().moveRuleDown(row++);
    }
    return row;
  }

  class MoveDownAction extends XAbstractAction {
    MoveDownAction() {
      super("Move down");
    }

    public void actionPerformed(ActionEvent ae) {
      moveDown();
    }
  }
}

class RuleDetailPanel extends XContainer {
  RuleDetailPanel(JCheckBox checkBox, JButton button) {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    add(checkBox, gbc);
    gbc.gridx++;
    add(button, gbc);

    setBorder(null);
  }

  public String getToolTipText(MouseEvent event) {
    setSize(-getX(), -getY());
    setLocation(0, 0);
    JComponent c = (JComponent) SwingUtilities.getDeepestComponentAt(this, event.getX(), event.getY());
    setLocation(-getWidth(), -getHeight());
    setSize(0, 0);
    return c != null ? c.getToolTipText() : null;
  }
}
