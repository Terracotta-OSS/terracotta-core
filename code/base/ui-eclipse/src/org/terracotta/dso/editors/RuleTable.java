/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.commons.lang.StringUtils;

import org.dijon.Button;
import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.Label;
import org.dijon.jspring.Layout;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XTableCellRenderer;
import org.terracotta.dso.dialogs.OnLoadDialog;
import com.terracottatech.configV2.Include;
import com.terracottatech.configV2.OnLoad;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

public class RuleTable extends XObjectTable {
  private OnLoadDialog m_onLoadDialog;
  
  private static final String MOVE_UP_ACTION   = "MoveUp";
  private static final String MOVE_DOWN_ACTION = "MoveDown";
  
  private static final KeyStroke MOVE_UP_STROKE =
    KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK);
  private static final KeyStroke MOVE_DOWN_STROKE =
    KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK);
  
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
    if(m_onLoadDialog == null) {
      Frame frame = (Frame)getAncestorOfClass(Frame.class);
      m_onLoadDialog = new OnLoadDialog(frame);
    }
    return m_onLoadDialog;
  }
  
  class RuleTypeRenderer extends XTableCellRenderer {
    public java.awt.Component getTableCellRendererComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      boolean hasFocus,
      int     row,
      int     col)
    {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
      
      Rule rule = getRuleAt(row);
      setText(rule.isExcludeRule() ? "Exclude" : "Include");

      return this;
    }
  }
  
  class RuleTypeEditor extends XCellEditor {
    private static final String INCLUDE_ITEM = "Include";
    private static final String EXCLUDE_ITEM = "Exclude";

    private final String[] MODEL_ITEMS = new String[] {INCLUDE_ITEM, EXCLUDE_ITEM};
    
    private int                  m_row;
    private DefaultComboBoxModel m_model;
    
    RuleTypeEditor() {
      super(new XComboBox());
      m_model = new DefaultComboBoxModel(MODEL_ITEMS);
      ((XComboBox)m_editorComponent).setModel(m_model);
    }
    
    protected void fireEditingStopped() {
      super.fireEditingStopped();
      setRowSelectionInterval(m_row, m_row);
    }
    
    public Object getCellEditorValue() {
      Object value = super.getCellEditorValue();
      return new Integer(value.equals(INCLUDE_ITEM) ? Rule.INCLUDE_RULE : Rule.EXCLUDE_RULE);
    }

    public java.awt.Component getTableCellEditorComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      int     row,
      int     col)
    {
      super.getTableCellEditorComponent(table, value, isSelected, row, col);
      
      Rule rule = getRuleAt(row);
      m_model.setSelectedItem(MODEL_ITEMS[rule.getType()]);
      m_row = row;
      
      return (java.awt.Component)m_editorComponent;
    }
  }
  
  class RuleDetailRenderer extends XTableCellRenderer {
    protected TableCellEditor m_editor;

    public RuleDetailRenderer() {
      super();
      m_editor = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new RuleDetailEditor(false);
    }

    public java.awt.Component getTableCellRendererComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      boolean hasFocus,
      int     row,
      int     col)
    {
      JComponent c = (JComponent)
        m_editor.getTableCellEditorComponent(table, value, isSelected, row, col);
      
      c.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : null);
      
      return c;
    }
  }
  
  private static String buildToolTip(Include include) {
    OnLoad onLoad = include.getOnLoad();
    String tip    = null;
    
    if(onLoad != null) {
      if((tip = onLoad.getMethod()) == null) {
        if((tip = onLoad.getExecute()) != null) {
          StringBuffer sb = new StringBuffer();
          String[] lines = StringUtils.split(onLoad.getExecute(),
                                             System.getProperty("line.separator"));
  
          sb.append("<html>");
          for(int i = 0; i < lines.length; i++) {
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
  
  class OnLoadButton extends Button {
    int m_row, m_col;
    
    OnLoadButton(String text) {
      super(text);
      setMargin(new Insets(0,2,0,2));
    }
    
    void setCell(int row, int col) {
      m_row = row;
      m_col = col;
    }

    int getRow() {return m_row;}
    int getCol() {return m_col;}
  }
  
  class RuleDetailEditor extends XCellEditor {
    OnLoadButton m_onLoadButton;
    XCheckBox    m_honorTransientToggle;
    Label        m_excludeRenderer;
    boolean      m_alwaysSelected;
    
    RuleDetailEditor(boolean alwaysSelected) {
      super(new XCheckBox("Honor transient"));

      m_alwaysSelected = alwaysSelected;
      
      m_honorTransientToggle = (XCheckBox)m_editorComponent;
      m_honorTransientToggle.addActionListener(new ActionListener () {
        public void actionPerformed(ActionEvent ae) {
          boolean honor = m_honorTransientToggle.isSelected();
          int     row   = m_onLoadButton.getRow();
          int     col   = m_onLoadButton.getCol();
          
          getIncludeRuleAt(row).setHonorTransient(honor);
          getRuleModel().fireTableCellUpdated(row, col);
        }
      });
      m_honorTransientToggle.setMargin(new Insets(0,0,0,0));
      
      m_onLoadButton = new OnLoadButton("On load...");
      m_onLoadButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          int     row          = m_onLoadButton.getRow();
          Include include      = getIncludeAt(row);
          String  savedInclude = include.xmlText();
          
          getOnLoadDialog().edit(include);
          if(!include.xmlText().equals(savedInclude)) {
            getRuleModel().fireTableCellUpdated(row, m_onLoadButton.getCol());
          }
        }
      });
      m_editorComponent = new RuleDetailPanel(m_honorTransientToggle, m_onLoadButton); 
      m_excludeRenderer = new Label("");

      m_clicksToStart = 1;
    }
    
    public java.awt.Component getTableCellEditorComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      int     row,
      int     col)
    {
      Rule rule = getRuleAt(row);
      
      if(m_alwaysSelected) {
        isSelected = true;
      }
      
      Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
      Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
      
      if(rule.isIncludeRule()) {
        Include include = getIncludeAt(row);
        
        m_onLoadButton.setCell(row, col);
        m_onLoadButton.setForeground(table.getForeground());
        m_onLoadButton.setBackground(table.getBackground());
        m_onLoadButton.setFont(table.getFont());
        m_onLoadButton.setToolTipText(buildToolTip(include));
        
        m_honorTransientToggle.setSelected(include.getHonorTransient());
        m_honorTransientToggle.setForeground(fg);
        m_honorTransientToggle.setBackground(bg);
        m_honorTransientToggle.setFont(table.getFont());
        m_honorTransientToggle.setOpaque(true);
        
        m_editorComponent.setForeground(fg);
        m_editorComponent.setBackground(bg);
        m_editorComponent.setOpaque(true);
        
        m_editorComponent.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

        return (java.awt.Component)m_editorComponent;
      }
      else {
        m_excludeRenderer.setForeground(fg);
        m_excludeRenderer.setBackground(bg);
        m_excludeRenderer.setOpaque(true);
        m_excludeRenderer.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
       
        return m_excludeRenderer;
      }
    }
  }
  
  public RuleModel getRuleModel() {
    return (RuleModel)getModel();
  }
  
  public Include getIncludeAt(int row) {
    return getIncludeRuleAt(row).getInclude();
  }
  
  public IncludeRule getIncludeRuleAt(int row) {
    return (IncludeRule)getRuleAt(row);
  }
  
  public Rule getRuleAt(int row) {
    return getRuleModel().getRuleAt(row);
  }

  public void moveUp() {
    int row = getSelectedRow();
    
    if(isEditing()) {
      removeEditor();
    }
    
    if(row > 0) {
      getRuleModel().moveRuleUp(row--);
      setRowSelectionInterval(row, row);
    }
  }
  
  class MoveUpAction extends XAbstractAction {
    MoveUpAction() {
      super("Move up");
    }
    
    public void actionPerformed(ActionEvent ae) {
      moveUp();
    }
  }

  public void moveDown() {
    int row = getSelectedRow();

    if(isEditing()) {
      removeEditor();
    }
    
    if(row != -1 && row < getRuleModel().getRowCount()-1) {
      getRuleModel().moveRuleDown(row++);
      setRowSelectionInterval(row, row);
    }
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

class RuleDetailPanel extends Container {
  RuleDetailPanel(CheckBox checkBox, Button button) {
    super();

    String[] constraint1 = {"-1,left:ns", "-1,top:s", "1,left:n",   "-1,bottom:s",  "n","n"};
    String[] constraint2 = {"0,right:n",  "-1,top:s", "-1,right:ns", "-1,bottom:s", "n","n"};

    setLayout(new Layout());
    add(checkBox, constraint1);
    add(button, constraint2);
    
    setBorder(null);
  }
  
  public String getToolTipText(MouseEvent event) {
    setSize(-getX(), -getY());
    setLocation(0, 0);
    JComponent c = (JComponent)SwingUtilities.getDeepestComponentAt(this, event.getX(), event.getY());
    setLocation(-getWidth(), -getHeight());
    setSize(0, 0);
    return c != null ? c.getToolTipText() : null;
  }
}
