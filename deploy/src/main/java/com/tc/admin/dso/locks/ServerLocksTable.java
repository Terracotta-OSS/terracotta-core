/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.XObjectTable;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class ServerLocksTable extends XObjectTable {
  private TableCellRenderer                     fTableColumnHeaderRenderer = new LockTableHeaderRenderer();

  private static final DefaultTableCellRenderer HEADER_RENDERER            = new DefaultTableCellRenderer();

  public ServerLocksTable() {
    super();
    setDefaultRenderer(Long.class, new StatValueRenderer());
  }

  class LockTableHeaderRenderer extends TableColumnRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      ServerLockTableModel model = (ServerLockTableModel) table.getModel();
      ((JComponent) c).setToolTipText(model.columnTip(column));
      return c;
    }
  }

  public void setModel(TableModel model) {
    super.setModel(model);
    TableColumnModel colModel = getColumnModel();
    for (int i = 0; i < colModel.getColumnCount(); i++) {
      colModel.getColumn(i).setHeaderRenderer(fTableColumnHeaderRenderer);
    }
  }

  public void addColumn(TableColumn aColumn) {
    super.addColumn(aColumn);
    aColumn.setHeaderRenderer(HEADER_RENDERER);
  }
}
