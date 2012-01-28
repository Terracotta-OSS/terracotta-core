/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.ArrowLabel;
import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XTableCellRenderer;
import com.tc.admin.common.treetable.JTreeTable;
import com.tc.admin.common.treetable.TreeTableModel;
import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

public class LockTreeTable extends JTreeTable {
  protected int                                 fSortColumn;
  protected int                                 fSortDirection;
  private TableColumnRenderer                   fColumnRenderer;
  private Preferences                           fPreferences;
  private ArrowLabel                            fArrowLabel;

  public static final int                       UP                      = SwingConstants.NORTH;
  public static final int                       DOWN                    = SwingConstants.SOUTH;

  private static final String                   SORT_COLUMN_PREF_KEY    = "SortColumn";
  private static final String                   SORT_DIRECTION_PREF_KEY = "SortDirection";

  private static final DefaultTableCellRenderer HEADER_RENDERER         = new DefaultTableCellRenderer();

  public LockTreeTable() {
    super();

    fArrowLabel = new ArrowLabel();
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    getTableHeader().addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          setSortDirection(toggleSortDirection());
          setSortColumn(columnAtPoint(me.getPoint()));
        }
      }
    });
    getTableHeader().setDefaultRenderer(fColumnRenderer = new TableColumnRenderer());
    setDefaultRenderer(Long.class, new StatValueRenderer());
  }

  LockTreeTable(TreeTableModel treeTableModel, Preferences prefs) {
    this();
    setTreeTableModel(treeTableModel);
    setPreferences(prefs);
  }

  public void setPreferences(Preferences prefs) {
    if ((fPreferences = prefs) != null) {
      fSortColumn = prefs.getInt(SORT_COLUMN_PREF_KEY, 1);
      fSortDirection = prefs.getInt(SORT_DIRECTION_PREF_KEY, DOWN);
    }
  }

  public void setTreeTableModel(TreeTableModel model) {
    super.setTreeTableModel(model);
    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
    renderer.setLeafIcon(null);
    renderer.setClosedIcon(null);
    renderer.setOpenIcon(null);

    if (getSortColumn() != -1) {
      sort();
    }
  }

  public JTree getTree() {
    return tree;
  }

  public void addTreeSelectionListener(TreeSelectionListener listener) {
    tree.addTreeSelectionListener(listener);
  }

  public void removeTreeSelectionListener(TreeSelectionListener listener) {
    tree.removeTreeSelectionListener(listener);
  }

  public void addColumn(TableColumn aColumn) {
    super.addColumn(aColumn);
    aColumn.setHeaderRenderer(HEADER_RENDERER);
  }

  private void putPreferenceInt(String key, int value) {
    if (fPreferences != null) {
      fPreferences.putInt(key, value);
      PrefsHelper.getHelper().flush(fPreferences);
    }
  }

  private void internalSetSortDirection(int direction) {
    fSortDirection = direction;
    putPreferenceInt(SORT_DIRECTION_PREF_KEY, direction);
  }

  public void setSortDirection(int direction) {
    internalSetSortDirection(direction);
  }

  public int getSortDirection() {
    return fSortDirection;
  }

  public int toggleSortDirection() {
    return fSortDirection == UP ? DOWN : UP;
  }

  private void internalSetSortColumn(int columnIndex) {
    if ((fSortColumn = columnIndex) != -1) {
      sort();
    }
    putPreferenceInt(SORT_COLUMN_PREF_KEY, columnIndex);
  }

  public void setSortColumn(int columnIndex) {
    internalSetSortColumn(columnIndex);
  }

  public int getSortColumn() {
    return fSortColumn;
  }

  public void sort() {
    if (fSortColumn != -1) {
      LockTreeTableModel model = (LockTreeTableModel) getTreeTableModel();
      JTableHeader header = getTableHeader();

      model.sort(fSortColumn, fSortDirection);

      if (header != null) {
        header.repaint();
      }
    }
  }

  class TableColumnRenderer extends XTableCellRenderer {
    private JComponent sortView;
    private Border     border;

    TableColumnRenderer() {
      super();
      sortView = new JComponent() {/**/
      };
      sortView.setLayout(new BorderLayout());

      border = Os.isMac() ? new BevelBorder(BevelBorder.RAISED) : UIManager.getBorder("TableHeader.cellBorder");
    }

    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int column) {
      if (table != null) {
        JTableHeader header = table.getTableHeader();

        if (header != null) {
          setForeground(header.getForeground());
          setBackground(header.getBackground());
          setFont(header.getFont());
        }
      }

      String text = (value == null) ? "" : value.toString();
      setText(text);
      String tip = ((LockTreeTableModel) getTreeTableModel()).getColumnTip(column);
      setToolTipText(tip);

      if (fSortColumn != -1) {
        if (column == fSortColumn) {
          fArrowLabel.setDirection(fSortDirection);

          setBorder(null);
          setOpaque(false);

          sortView.setForeground(null);
          sortView.setBackground(null);
          sortView.setFont(null);

          sortView.add(this, BorderLayout.WEST);
          sortView.add(fArrowLabel, BorderLayout.EAST);
          sortView.setBorder(border);
          sortView.setToolTipText(tip);
          return sortView;
        }
      }

      setBorder(border);

      return this;
    }
  }

  public void createDefaultColumnsFromModel() {
    super.createDefaultColumnsFromModel();

    TableColumnModel colModel = getColumnModel();
    TableColumn column;

    for (int i = 0; i < colModel.getColumnCount(); i++) {
      column = colModel.getColumn(i);
      column.setHeaderRenderer(fColumnRenderer);
    }
  }
}
