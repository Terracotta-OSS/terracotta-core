/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class XTable extends JTable {
  protected XPopupListener    popupHelper;
  protected Timer             columnPrefsTimer;

  private static final String COLUMNS_PREF_KEY = "Columns";

  public XTable() {
    super();
    init();
  }

  public XTable(TableModel model) {
    super(model);
    init();
  }

  private void init() {
    setDefaultRenderer(Integer.class, new IntegerRenderer());
    setDefaultRenderer(Long.class, new LongRenderer());
    setDefaultRenderer(Date.class, new DateRenderer());
    setDefaultRenderer(Float.class, new FloatRenderer());
    setDefaultRenderer(Double.class, new FloatRenderer());

    popupHelper = new XPopupListener(this);

    columnPrefsTimer = new Timer(1000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        storeColumnPrefs();
      }
    });
    columnPrefsTimer.setRepeats(false);

    getTableHeader().setReorderingAllowed(false);
  }

  @Override
  public void addNotify() {
    super.addNotify();

    TableColumnModel colModel = getColumnModel();
    if (colModel != null && colModel.getColumnCount() > 0) {
      loadColumnPrefs();
    }
  }

  public static class BaseRenderer extends AbstractTableCellRenderer {
    protected Format formatter;
    protected XLabel label;

    public BaseRenderer() {
      this((Format) null);
    }

    public BaseRenderer(String format) {
      this(new DecimalFormat(format));
    }

    public BaseRenderer(Format formatter) {
      super();
      this.formatter = formatter;
      label = new XLabel();
      label.setOpaque(true);
    }

    protected void setText(String text) {
      label.setText(text);
    }

    protected void setIcon(Icon icon) {
      label.setIcon(icon);
    }

    @Override
    public void setValue(Object value) {
      String text = "";

      try {
        if (value != null) {
          text = formatter != null ? formatter.format(value) : value.toString();
        }
      } catch (Exception nfe) {
        System.out.println(value.toString());
      }

      setText(text);
    }

    @Override
    public JComponent getComponent() {
      return label;
    }
  }

  public static class IntegerRenderer extends BaseRenderer {
    public IntegerRenderer() {
      super("#,##0;(#,##0)");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public static class PortNumberRenderer extends BaseRenderer {
    public PortNumberRenderer() {
      super("###0;(###0)");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public static class LongRenderer extends BaseRenderer {
    public LongRenderer() {
      super("#,##0;(#,##0)");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public static class DateRenderer extends BaseRenderer {
    public DateRenderer() {
      super(DateFormat.getDateTimeInstance());
    }
  }

  public static class FloatRenderer extends BaseRenderer {
    public FloatRenderer() {
      super("#,##0.00####;(#,##0.00####)");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public static class PercentRenderer extends BaseRenderer {
    public PercentRenderer() {
      super("##%");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    popupHelper.setPopupMenu(popupMenu);
  }

  public JPopupMenu getPopupMenu() {
    return popupHelper.getPopupMenu();
  }

  @Override
  public void setModel(TableModel model) {
    super.setModel(model);

    TableColumnModel colModel = getColumnModel();
    if (colModel != null && colModel.getColumnCount() > 0) {
      loadColumnPrefs();
    }
  }

  protected void loadColumnPrefs() {
    if (getClass().equals(XTable.class)) { return; }

    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = helper.userNodeForClass(getClass());
    TableColumnModel colModel = getColumnModel();
    String s = prefs.get(COLUMNS_PREF_KEY, null);
    int width;

    if (s != null) {
      String[] split = s.split(",");

      for (int i = 0; i < colModel.getColumnCount(); i++) {
        if (i < split.length && split[i] != null) {
          try {
            width = Integer.parseInt(split[i]);
            colModel.getColumn(i).setPreferredWidth(width);
          } catch (Exception e) {/**/
          }
        }
      }
    }
  }

  protected void storeColumnPrefs() {
    if (getClass().equals(XTable.class)) { return; }

    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = helper.userNodeForClass(getClass());
    StringBuffer sb = new StringBuffer();
    TableColumnModel colModel = getColumnModel();
    int width;

    for (int i = 0; i < colModel.getColumnCount(); i++) {
      width = colModel.getColumn(i).getWidth();
      sb.append(width);
      sb.append(",");
    }

    String s = sb.substring(0, sb.length() - 1);
    prefs.put(COLUMNS_PREF_KEY, s);
    helper.flush(prefs);
  }

  @Override
  public void columnMarginChanged(ChangeEvent e) {
    boolean isValid = isValid();

    if (isValid) {
      columnPrefsTimer.stop();
    }
    super.columnMarginChanged(e);
    if (isValid) {
      columnPrefsTimer.start();
    }
  }

  @Override
  public Dimension getPreferredSize() {
    int rowCount = getRowCount();

    if (rowCount > 0) {
      int columnCount = getColumnCount();
      TableCellRenderer renderer;
      java.awt.Component comp;
      Dimension prefSize;
      int height = 0;

      for (int row = 0; row < rowCount; row++) {
        for (int col = 0; col < columnCount; col++) {
          if ((renderer = getCellRenderer(row, col)) != null) {
            comp = renderer.getTableCellRendererComponent(this, getValueAt(row, col), true, true, row, col);

            prefSize = comp.getPreferredSize();
            height = Math.max(height, prefSize.height);
          }
        }
      }

      if (height > 10) {
        setRowHeight(height);
      }
    }

    return super.getPreferredSize();
  }

  public void setSelectedRows(int[] rows) {
    int rowCount = getRowCount();
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] < rowCount) {
        setRowSelectionInterval(rows[i], rows[i]);
      }
    }
  }

  public void setSelectedRow(int row) {
    setSelectedRows(new int[] { row });
    Rectangle cellRect = getCellRect(row, 0, true);
    if (cellRect != null) {
      scrollRectToVisible(cellRect);
    }
  }
}
