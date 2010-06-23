/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.util.runtime.Os;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class XObjectTable extends XTable {
  protected int               sortColumn;
  protected int               sortDirection;

  private TableColumnRenderer columnRenderer;

  public static final int     UP                      = XObjectTableModel.UP;
  public static final int     DOWN                    = XObjectTableModel.DOWN;

  private ArrowLabel          arrowLabel;

  private static final String SORT_COLUMN_PREF_KEY    = "SortColumn";
  private static final String SORT_DIRECTION_PREF_KEY = "SortDirection";

  public XObjectTable() {
    super();
    init();
  }

  public XObjectTable(TableModel model) {
    super(model);
    init();
  }

  private void init() {
    sortColumn = -1;
    sortDirection = DOWN;

    arrowLabel = new ArrowLabel();

    setDefaultRenderer(Method.class, new MethodRenderer());
    setDefaultEditor(Method.class, new MethodEditor());

    getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          int col = columnAtPoint(me.getPoint());

          if (((XObjectTableModel) getModel()).isColumnSortable(col)) {
            setSortDirection(toggleSortDirection());
            setSortColumn(col);
          }
        }
      }
    });
    columnRenderer = new TableColumnRenderer();

    getTableHeader().setDefaultRenderer(columnRenderer);
  }

  @Override
  public void createDefaultColumnsFromModel() {
    super.createDefaultColumnsFromModel();

    XObjectTableModel tableModel = (XObjectTableModel) getModel();
    TableColumnModel colModel = getColumnModel();
    TableColumn column;

    for (int i = 0; i < colModel.getColumnCount(); i++) {
      column = colModel.getColumn(i);
      column.setHeaderRenderer(columnRenderer);
      column.setIdentifier(tableModel.getFieldName(i));
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    loadSortPrefs();
  }

  protected class TableColumnRenderer extends XTableCellRenderer {
    private final JComponent sortView;
    private final Border     border;

    public TableColumnRenderer() {
      super();
      sortView = new JComponent() {/**/};
      sortView.setLayout(new BorderLayout());

      border = Os.isMac() ? new BevelBorder(BevelBorder.RAISED) : UIManager.getBorder("TableHeader.cellBorder");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
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

      if (sortColumn != -1) {
        if (column == sortColumn) {
          arrowLabel.setDirection(sortDirection);

          setBorder(null);
          setOpaque(false);

          sortView.setForeground(null);
          sortView.setBackground(null);
          sortView.setFont(null);

          sortView.add(this, BorderLayout.WEST);
          sortView.add(arrowLabel, BorderLayout.EAST);
          sortView.setBorder(border);
          return sortView;
        }
      }

      setBorder(border);
      return this;
    }
  }

  private void internalSetSortColumn(int columnIndex) {
    sortColumn = Math.min(columnIndex, getModel().getColumnCount() - 1);
    if (sortColumn != -1) {
      sort();
    }
  }

  public void setSortColumn(int columnIndex) {
    internalSetSortColumn(columnIndex);
    storeSortPrefs();
  }

  public int getSortColumn() {
    return sortColumn;
  }

  public void sort() {
    if (sortColumn != -1) {
      TableModel model = getModel();
      JTableHeader header = getTableHeader();

      if (model != null && model instanceof XObjectTableModel) {
        Object[] selection = getSelection();

        ((XObjectTableModel) model).sortColumn(sortColumn, sortDirection);

        if (header != null) {
          header.repaint();
        }

        setSelection(selection);
      }
    }
  }

  private void internalSetSortDirection(int direction) {
    sortDirection = direction;
  }

  public void setSortDirection(int direction) {
    internalSetSortDirection(direction);
    storeSortPrefs();
  }

  public int getSortDirection() {
    return sortDirection;
  }

  public int toggleSortDirection() {
    return sortDirection == UP ? DOWN : UP;
  }

  public Object[] getSelection() {
    Object[] result = {};
    XObjectTableModel model = (XObjectTableModel) getModel();
    if (model.getRowCount() > 0) {
      int[] rows = getSelectedRows();
      result = new Object[rows.length];
      for (int i = 0; i < rows.length; i++) {
        result[i] = model.getObjectAt(rows[i]);
      }
    }
    return result;
  }

  public void setSelection(Object[] selection) {
    XObjectTableModel model = (XObjectTableModel) getModel();
    int index;

    clearSelection();

    for (int i = 0; i < selection.length; i++) {
      index = model.getObjectIndex(selection[i]);
      addRowSelectionInterval(index, index);
    }
  }

  @Override
  protected TableModel createDefaultDataModel() {
    return new XObjectTableModel();
  }

  @Override
  public void setModel(TableModel model) {
    super.setModel(model);
    if (sortColumn != -1) {
      sortColumn = Math.min(sortColumn, model.getColumnCount() - 1);
      sort();
    }
  }

  public void showColumnsExclusive(String[] fieldNames) {
    Object[] selection = getSelection();
    XObjectTableModel model = (XObjectTableModel) getModel();

    model.showColumnsExclusive(fieldNames);
    setSelection(selection);
  }

  public void showColumn(String fieldName) {
    Object[] selection = getSelection();
    XObjectTableModel model = (XObjectTableModel) getModel();

    model.showColumn(fieldName);
    setSelection(selection);
  }

  public void hideColumn(String fieldName) {
    Object[] selection = getSelection();
    XObjectTableModel model = (XObjectTableModel) getModel();

    model.hideColumn(fieldName);
    if (getSortColumn() >= getColumnCount()) {
      setSortColumn(getColumnCount() - 1);
    }
    setSelection(selection);
  }

  public TableColumn findColumn(String fieldName) {
    XObjectTableModel model = (XObjectTableModel) getModel();
    int index = model.getShowingFieldIndex(fieldName);

    return index != -1 ? getColumnModel().getColumn(index) : null;
  }

  public int getShowingFieldCount() {
    return getColumnCount();
  }

  public String[] getShowingFields() {
    return ((XObjectTableModel) getModel()).getShowingFields();
  }

  public boolean isColumnShowing(String fieldName) {
    return ((XObjectTableModel) getModel()).isColumnShowing(fieldName);
  }

  class MethodRenderer extends XTableCellRenderer {
    protected TableCellEditor editor;

    public MethodRenderer() {
      super();
      editor = createCellEditor();
    }

    protected TableCellEditor createCellEditor() {
      return new MethodEditor();
    }

    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int col) {
      return editor.getTableCellEditorComponent(table, value, false, row, col);
    }
  }

  class MethodEditor extends XCellEditor {
    InvokerButton button;

    MethodEditor() {
      super(new XCheckBox());

      editorComponent = button = new InvokerButton();
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          int row = button.getRow();
          int col = button.getCol();
          XObjectTableModel model = (XObjectTableModel) getModel();
          Method method = (Method) model.getValueAt(row, col);
          Object obj = model.getObjectAt(row);

          try {
            method.invoke(obj, new Object[] {});
            XObjectTable.this.repaint();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      clickCountToStart = 1;
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                          int col) {
      super.getTableCellEditorComponent(table, value, isSelected, row, col);

      XObjectTableModel model = (XObjectTableModel) table.getModel();
      Method method = (Method) model.getValueAt(row, col);

      button.setText(method.getName());
      button.setCell(row, col);
      button.setForeground(table.getForeground());
      button.setBackground(table.getBackground());
      button.setFont(table.getFont());

      return button;
    }
  }

  protected void loadSortPrefs() {
    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = helper.userNodeForClass(getClass());
    String s;

    // It's important that we set the sortDirection prior to the sortColumn
    // because the latter does an actual sort.

    if ((s = prefs.get(SORT_DIRECTION_PREF_KEY, null)) != null) {
      try {
        internalSetSortDirection(Integer.parseInt(s));
      } catch (NumberFormatException nfe) {/**/
      }
    }

    if ((s = prefs.get(SORT_COLUMN_PREF_KEY, null)) != null) {
      try {
        internalSetSortColumn(Integer.parseInt(s));
      } catch (NumberFormatException nfe) {/**/
      }
    }
  }

  protected void storeSortPrefs() {
    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = helper.userNodeForClass(getClass());

    prefs.put(SORT_COLUMN_PREF_KEY, Integer.toString(getSortColumn()));
    prefs.put(SORT_DIRECTION_PREF_KEY, Integer.toString(getSortDirection()));

    helper.flush(prefs);
  }

  private static class InvokerButton extends XButton {
    private int row;
    private int col;

    public InvokerButton() {
      super();

      setFocusable(false);
      setOpaque(true);
    }

    void setCell(int row, int col) {
      setRow(row);
      setCol(col);
    }

    void setRow(int row) {
      this.row = row;
    }

    int getRow() {
      return row;
    }

    void setCol(int col) {
      this.col = col;
    }

    int getCol() {
      return col;
    }

    // public Dimension _getPreferredSize() {
    // Dimension d = super.getPreferredSize();
    //
    // if (true || System.getProperty("os.name").equals("Mac OS X")) {
    // d.height = 20;
    // }
    //
    // return d;
    // }

  }
}
