/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.servers;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.TextField;

import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XTableCellRenderer;
import com.tc.admin.common.XTextField;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class ServerEnvTable extends XObjectTable {
  private TableCellRenderer m_renderer;
  private TableCellEditor   m_editor;
  private JFileChooser      m_chsr;
  private File              m_lastDir;

  protected static Border noFocusBorder = new EmptyBorder(1,1,1,1); 
  
  public ServerEnvTable() {
    super();
  }
  
  public TableCellRenderer getCellRenderer(int row, int col) {
    if(col == 1) {
      if(m_renderer == null) {
        m_renderer = new ValueRenderer();
      }
      return m_renderer;
    }
    return super.getCellRenderer(row, col);
  }
  
  public TableCellEditor getCellEditor(int row, int col) {
    if(col == 1) {
      if(m_editor == null) {
        m_editor = new ValueEditor();
      }
      return m_editor;
    }
    return super.getCellEditor(row, col);
  }
  
  class ValueRenderer extends XTableCellRenderer {
    protected TableCellEditor m_rendererDelegate;

    public ValueRenderer() {
      super();
      m_rendererDelegate = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new ValueEditor(true);
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
        m_rendererDelegate.getTableCellEditorComponent(table, value, isSelected, row, col);
      
      c.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : null);
      
      return c;
    }
  }
  
  class ChooserButton extends Button {
    int m_row, m_col;
    
    ChooserButton(String text) {
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
  
  private JFileChooser getChooser() {
    if(m_chsr == null) {
      m_chsr = new JFileChooser();
      m_chsr.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    
    if(m_lastDir != null) {
      m_chsr.setCurrentDirectory(m_lastDir);
    }
    
    return m_chsr;
  }
  
  class ValueEditor extends XCellEditor {
    ChooserButton m_chsrButton;
    XTextField    m_valueField;
    boolean       m_isRenderer;
    
    ValueEditor() {
      this(false);
    }
    
    ValueEditor(boolean isRenderer) {
      super(new XTextField());

      m_isRenderer = isRenderer;
      
      m_valueField = (XTextField)m_editorComponent;
      m_valueField.addActionListener(new ActionListener () {
        public void actionPerformed(ActionEvent ae) {
          int row = m_chsrButton.getRow();
          int col = m_chsrButton.getCol();
          
          getServerPropertyAt(row).setValue(m_valueField.getText());
          getServerEnvTableModel().fireTableCellUpdated(row, col);
        }
      });
      m_valueField.setMargin(new Insets(0,0,0,0));
      
      m_chsrButton = new ChooserButton("...");
      m_chsrButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JFileChooser chsr = getChooser();
          
          if(chsr.showOpenDialog(ServerEnvTable.this) == JFileChooser.APPROVE_OPTION) {
            int    row  = m_chsrButton.getRow();
            int    col  = m_chsrButton.getCol();
            String path = chsr.getSelectedFile().getAbsolutePath();

            removeEditor();
            getServerEnvTableModel().setValueAt(path, row, col);
          } else {
            removeEditor();
          }
          m_lastDir = chsr.getCurrentDirectory();
        }
      });

      m_editorComponent = new PropertyValuePanel(m_valueField, m_chsrButton); 
      m_clicksToStart   = 1;
    }
    
    public java.awt.Component getTableCellEditorComponent(
      JTable  table,
      Object  value,
      boolean isSelected,
      int     row,
      int     col)
    {
      Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
      Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
      
      ServerProperty prop = getServerPropertyAt(row);
      
      m_chsrButton.setCell(row, col);
      m_chsrButton.setForeground(table.getForeground());
      m_chsrButton.setBackground(table.getBackground());
      m_chsrButton.setFont(table.getFont());
      
      m_valueField.setText(prop.getValue());
      if(!m_isRenderer) {
        m_valueField.setForeground(UIManager.getColor("TextField.foreground"));
        m_valueField.setBackground(UIManager.getColor("TextField.background"));
      }
      else {
        m_valueField.setForeground(fg);
        m_valueField.setBackground(bg);
      }
      m_valueField.setFont(table.getFont());
      
      if(!m_isRenderer) {
        m_valueField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      }
      else {
        m_valueField.setBorder(noFocusBorder);
        m_chsrButton.setVisible(false);
      }

      return (java.awt.Component)m_editorComponent;
    }
  }

  public ServerEnvTableModel getServerEnvTableModel() {
    return (ServerEnvTableModel)getModel();
  }
  
  public ServerProperty getServerPropertyAt(int row) {
    return getServerEnvTableModel().getServerPropertyAt(row);
  }
}

class PropertyValuePanel extends Container {
  PropertyValuePanel(TextField textfield, Button button) {
    super();
    setLayout(new BorderLayout());
    add(textfield);
    add(button, BorderLayout.EAST);
    setBorder(null);
  }
}
