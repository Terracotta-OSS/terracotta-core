/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.commons.lang.StringUtils;

import org.dijon.Button;

import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XTableCellRenderer;
import org.terracotta.dso.dialogs.OnLoadDialog;
import com.terracottatech.configV2.Include;
import com.terracottatech.configV2.OnLoad;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class IncludesTable extends XObjectTable {
  private OnLoadDialog m_onLoadDialog;
  
  public IncludesTable() {
    super();
    setDefaultRenderer(OnLoad.class, new OnLoadRenderer());
    setDefaultEditor(OnLoad.class, new OnLoadEditor());
  }
  
  public OnLoadDialog getOnLoadDialog() {
    if(m_onLoadDialog == null) {
      Frame frame = (Frame)getAncestorOfClass(Frame.class);
      m_onLoadDialog = new OnLoadDialog(frame);
    }
    return m_onLoadDialog;
  }
  
  class OnLoadRenderer extends XTableCellRenderer {
    protected TableCellEditor m_editor;

    public OnLoadRenderer() {
      super();
      m_editor = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new OnLoadEditor();
    }

    public java.awt.Component getTableCellRendererComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      boolean hasFocus,
      int     row,
      int     col)
    {
      return m_editor.getTableCellEditorComponent(table, value, false, row, col);
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
      setMargin(new Insets(0,0,0,0));
    }
    
    void setCell(int row, int col) {
      m_row = row;
      m_col = col;
    }

    int getRow() {return m_row;}
    int getCol() {return m_col;}
  }
  
  class OnLoadEditor extends XCellEditor {
    OnLoadButton button;

    OnLoadEditor() {
      super(new XCheckBox());

      m_editorComponent = button = new OnLoadButton("Edit...");
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          int row = button.getRow();
          int col = button.getCol();
          
          getOnLoadDialog().edit(getIncludeAt(row));
          XObjectTableModel model = (XObjectTableModel)getModel();
          model.fireTableCellUpdated(row, col);
        }
      });
      m_clicksToStart = 1;
    }
    
    public java.awt.Component getTableCellEditorComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      int     row,
      int     col)
    {
      super.getTableCellEditorComponent(table, value, isSelected, row, col);

      button.setCell(row, col);
      button.setForeground(table.getForeground());
      button.setBackground(table.getBackground());
      button.setFont(table.getFont());
      button.setToolTipText(buildToolTip(getIncludeAt(row)));
      
      return button;
    }
  }
  
  public Include getIncludeAt(int row) {
    XObjectTableModel model = (XObjectTableModel)getModel();
    return (Include)model.getObjectAt(row);
  }
}
