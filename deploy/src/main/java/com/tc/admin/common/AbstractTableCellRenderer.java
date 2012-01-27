/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public abstract class AbstractTableCellRenderer implements TableCellRenderer {
  protected static final Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

  public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                          boolean hasFocus, int row, int col) {
    JComponent comp = getComponent();

    comp.setOpaque(true);

    if (isSelected) {
      comp.setForeground(table.getSelectionForeground());
      comp.setBackground(table.getSelectionBackground());
    } else {
      comp.setForeground(table.getForeground());
      comp.setBackground(table.getBackground());
    }

    comp.setFont(table.getFont());

    if (hasFocus) {
      Border border = null;

      if (isSelected) {
        border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
      }
      if (border == null) {
        border = UIManager.getBorder("Table.focusCellHighlightBorder");
      }
      comp.setBorder(border);

      if (!isSelected && table.isCellEditable(row, col)) {
        Color color;

        if ((color = UIManager.getColor("Table.focusCellForeground")) != null) {
          comp.setForeground(color);
        }
        if ((color = UIManager.getColor("Table.focusCellBackground")) != null) {
          comp.setBackground(color);
        }
      }
    } else {
      comp.setBorder(noFocusBorder);
    }

    setValue(table, row, col);

    return comp;
  }

  public abstract JComponent getComponent();

  public void setValue(JTable table, int row, int col) {
    try {
      Method m = table.getClass().getMethod("convertRowIndexToModel", new Class[] { Integer.TYPE });
      if (m != null) {
        row = ((Integer) m.invoke(table, Integer.valueOf(row))).intValue();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      /**/
    }

    setValue(table.getModel().getValueAt(row, col));
  }

  public void setValue(Object value) {
    /**/
  }

  public static class UIResource extends DefaultTableCellRenderer implements javax.swing.plaf.UIResource {/**/
  }
}
