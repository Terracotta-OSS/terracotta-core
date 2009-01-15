/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.servers;

import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCellEditor;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XTableCellRenderer;
import com.tc.admin.common.XTextField;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class ServerEnvTable extends XObjectTable {
  private TableCellRenderer renderer;
  private TableCellEditor   editor;
  private JFileChooser      chsr;
  private File              lastDir;

  protected static Border   noFocusBorder = new EmptyBorder(1, 1, 1, 1);

  public ServerEnvTable() {
    super();
  }

  public TableCellRenderer getCellRenderer(int row, int col) {
    if (col == 1) {
      if (renderer == null) {
        renderer = new ValueRenderer();
      }
      return renderer;
    }
    return super.getCellRenderer(row, col);
  }

  public TableCellEditor getCellEditor(int row, int col) {
    if (col == 1) {
      if (editor == null) {
        editor = new ValueEditor();
      }
      return editor;
    }
    return super.getCellEditor(row, col);
  }

  class ValueRenderer extends XTableCellRenderer {
    protected TableCellEditor rendererDelegate;

    public ValueRenderer() {
      super();
      rendererDelegate = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new ValueEditor(true);
    }

    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int col) {
      JComponent c = (JComponent) rendererDelegate.getTableCellEditorComponent(table, value, isSelected, row, col);

      c.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : null);

      return c;
    }
  }

  class ChooserButton extends XButton {
    int row, col;

    ChooserButton(String text) {
      super(text);
      setMargin(new Insets(0, 2, 0, 2));
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

  private JFileChooser getChooser() {
    if (chsr == null) {
      chsr = new FastFileChooser();
      chsr.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    if (lastDir != null) {
      chsr.setCurrentDirectory(lastDir);
    }

    return chsr;
  }

  class ValueEditor extends XCellEditor {
    ChooserButton chsrButton;
    XTextField    valueField;
    boolean       isRenderer;

    ValueEditor() {
      this(false);
    }

    ValueEditor(boolean isRenderer) {
      super(new XTextField());

      this.isRenderer = isRenderer;

      valueField = (XTextField) editorComponent;
      valueField.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          int row = chsrButton.getRow();
          int col = chsrButton.getCol();

          getServerPropertyAt(row).setValue(valueField.getText());
          getServerEnvTableModel().fireTableCellUpdated(row, col);
        }
      });
      valueField.setMargin(new Insets(0, 0, 0, 0));

      chsrButton = new ChooserButton("...");
      chsrButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JFileChooser fileChooser = getChooser();
          if (fileChooser.showOpenDialog(ServerEnvTable.this) == JFileChooser.APPROVE_OPTION) {
            int row = chsrButton.getRow();
            int col = chsrButton.getCol();
            String path = fileChooser.getSelectedFile().getAbsolutePath();

            removeEditor();
            getServerEnvTableModel().setValueAt(path, row, col);
          } else {
            removeEditor();
          }
          lastDir = fileChooser.getCurrentDirectory();
        }
      });

      editorComponent = new PropertyValuePanel(valueField, chsrButton);
      clickCountToStart = 1;
    }

    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                          int col) {
      Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
      Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();

      ServerProperty prop = getServerPropertyAt(row);

      chsrButton.setCell(row, col);
      chsrButton.setForeground(table.getForeground());
      chsrButton.setBackground(table.getBackground());
      chsrButton.setFont(table.getFont());

      valueField.setText(prop.getValue());
      if (!isRenderer) {
        valueField.setForeground(UIManager.getColor("TextField.foreground"));
        valueField.setBackground(UIManager.getColor("TextField.background"));
      } else {
        valueField.setForeground(fg);
        valueField.setBackground(bg);
      }
      valueField.setFont(table.getFont());

      if (!isRenderer) {
        valueField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      } else {
        valueField.setBorder(noFocusBorder);
        chsrButton.setVisible(false);
      }

      return editorComponent;
    }
  }

  public ServerEnvTableModel getServerEnvTableModel() {
    return (ServerEnvTableModel) getModel();
  }

  public ServerProperty getServerPropertyAt(int row) {
    return getServerEnvTableModel().getServerPropertyAt(row);
  }
}

class PropertyValuePanel extends XContainer {
  PropertyValuePanel(JTextField textfield, JButton button) {
    super();
    setLayout(new BorderLayout());
    add(textfield);
    add(button, BorderLayout.EAST);
    setBorder(null);
  }
}
