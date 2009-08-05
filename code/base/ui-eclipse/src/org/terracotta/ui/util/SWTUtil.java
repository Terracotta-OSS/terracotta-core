/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.terracotta.dso.TcPlugin;

public final class SWTUtil {

  private SWTUtil() {
    // cannot instantiate
  }

  public static void makeIntField(final Text text) {
    text.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        if (e.widget.getDisplay() == null) {
          e.widget.removeListener(SWT.Verify, this);
          return;
        }
        String string = e.text;
        char[] chars = new char[string.length()];
        string.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
      }
    });
  }

  public static void setBGColorRecurse(Color color, Control control) {
    control.setBackground(color);
    if (control instanceof Composite) {
      Control[] children = ((Composite) control).getChildren();
      for (int i = 0; i < children.length; i++) {
        setBGColorRecurse(color, children[i]);
      }
    }
  }

  public static Control getAncestorOfClass(Class clazz, Control comp) {
    if (comp.getClass().equals(clazz)) return comp;
    while ((comp = comp.getParent()) != null) {
      if (clazz.isAssignableFrom(comp.getClass())) return comp;
    }
    return null;
  }

  public static int textColumnsToPixels(Control control, int columns) {
    GC gc = new GC(control);
    FontMetrics fm = gc.getFontMetrics();
    int width = columns * fm.getAverageCharWidth();
    int height = fm.getHeight();
    gc.dispose();
    return control.computeSize(width, height).x;
  }

  public static int textRowsToPixels(Control control, int rows) {
    GC gc = new GC(control);
    FontMetrics fm = gc.getFontMetrics();
    int height = rows * fm.getHeight();
    gc.dispose();
    return control.computeSize(0, height).y;
  }

  public static int tableRowsToPixels(Table table, int rows) {
    return table.getHeaderHeight() + (rows * table.getItemHeight());
  }

  public static void applyDefaultButtonSize(Button button) {
    Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
    Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(hint).applyTo(button);
  }

  public static void placeDialogInCenter(Shell parent, Shell shell) {
    Rectangle parentSize = parent.getBounds();
    Rectangle mySize = shell.getBounds();
    int locationX, locationY;
    locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
    locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;
    shell.setLocation(new Point(locationX, locationY));
  }

  public static void makeTableColumnsResizeEqualWidth(final Composite tablePanel, final Table table) {
    final Control control = table;
    control.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        if (e.widget.getDisplay() == null) {
          ((Control) e.widget).removeControlListener(this);
          return;
        }
        Rectangle area = control.getBounds();
        int widthHint = SWT.DEFAULT;
        int heightHint = SWT.DEFAULT;
        Point preferredSize = table.computeSize(widthHint, heightHint);
        int width = area.width - 2 * table.getBorderWidth();
        if (preferredSize.y > area.height + table.getHeaderHeight()) {
          Point vBarSize = table.getVerticalBar().getSize();
          width -= vBarSize.x + 1; // don't know why +1 is needed, but it is
        }
        TableColumn[] columns = table.getColumns();
        int colWidth = width / columns.length;
        for (int i = 0; i < columns.length; i++) {
          columns[i].setWidth(colWidth);
        }
      }
    });
  }

  public static TableWeightedResizeHandler makeTableColumnsResizeWeightedWidth(Composite tablePanel, Table table,
                                                                               int[] columnWeights) {
    TableWeightedResizeHandler result = new TableWeightedResizeHandler(tablePanel, table, columnWeights);
    table.addControlListener(result);
    return result;
  }

  public static class TableWeightedResizeHandler extends ControlAdapter {
    Composite tablePanel;
    Table     table;
    int[]     columnWeights;
    int       totalWeight;

    public TableWeightedResizeHandler(Composite tablePanel, Table table, int[] columnWeights) {
      this.tablePanel = tablePanel;
      this.table = table;
      setColumnWeights(columnWeights);
    }

    public void setColumnWeights(int[] columnWeights) {
      this.columnWeights = columnWeights;
      for (int i = 0; i < columnWeights.length; i++) {
        totalWeight += columnWeights[i];
      }
    }

    @Override
    public void controlResized(ControlEvent e) {
      if (table.isDisposed() || table.getDisplay() == null) {
        ((Control) e.widget).removeControlListener(this);
        return;
      }
      Rectangle area = tablePanel.getClientArea();
      Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      int width = area.width - 2 * table.getBorderWidth();
      if ((preferredSize.y + table.getHeaderHeight()) > area.height) {
        Point vBarSize = table.getVerticalBar().getSize();
        width -= vBarSize.x + 1; // don't know why +1 is needed, but it is
      }
      TableColumn[] columns = table.getColumns();
      int widthUnit = width / totalWeight;
      for (int i = 0; i < columns.length; i++) {
        columns[i].setWidth(widthUnit * columnWeights[i]);
      }
    }

    public void dispose() {
      table.removeControlListener(this);
      table = null;
      tablePanel = null;
    }
  }

  public static void makeTableColumnsEditable(final Table table, final int[] indices) {
    final TableEditor editor = new TableEditor(table);
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    table.addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        if (event.widget.getDisplay() == null) {
          event.widget.removeListener(SWT.MouseDown, this);
          return;
        }
        Rectangle clientArea = table.getClientArea();
        Point pt = new Point(event.x, event.y);
        int index = table.getTopIndex();
        while (index < table.getItemCount()) {
          boolean visible = false;
          final TableItem item = table.getItem(index);
          for (int i = 0; i < indices.length; i++) {
            Rectangle rect = item.getBounds(indices[i]);
            if (rect.contains(pt)) {
              final int column = indices[i];
              final Text text = new Text(table, SWT.NONE);
              final String initialValue = item.getText(indices[i]);
              Listener textListener = new Listener() {
                public void handleEvent(final Event e) {
                  Event updateEvent = new Event();
                  switch (e.type) {
                    case SWT.FocusOut:
                      item.setText(column, text.getText());
                      if (!initialValue.equals(text.getText())) {
                        updateEvent.item = item;
                        updateEvent.index = column;
                        table.notifyListeners(SWT.SetData, updateEvent);
                      }
                      text.dispose();
                      break;
                    case SWT.Traverse:
                      switch (e.detail) {
                        case SWT.TRAVERSE_RETURN:
                          item.setText(column, text.getText());
                          if (!initialValue.equals(text.getText())) {
                            updateEvent.item = item;
                            updateEvent.index = column;
                            table.notifyListeners(SWT.SetData, updateEvent);
                          }
                          text.dispose();
                          e.doit = false;
                          break;
                        case SWT.TRAVERSE_ESCAPE:
                          text.dispose();
                          e.doit = false;
                          break;
                      }
                      break;
                  }
                }
              };
              text.addListener(SWT.FocusOut, textListener);
              text.addListener(SWT.Traverse, textListener);
              editor.setEditor(text, item, indices[i]);
              text.setText(initialValue);
              text.setFocus();
              return;
            }
            if (!visible && rect.intersects(clientArea)) {
              visible = true;
            }
          }
          if (!visible) return;
          index++;
        }
      }
    });
  }

  public static void makeTableComboItem(final Table table, final int column, final String[] values) {
    final TableEditor editor = new TableEditor(table);
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    table.addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        if (event.widget.getDisplay() == null) {
          event.widget.removeListener(SWT.MouseDown, this);
          return;
        }
        Rectangle clientArea = table.getClientArea();
        Point pt = new Point(event.x, event.y);
        int index = table.getTopIndex();
        while (index < table.getItemCount()) {
          boolean visible = false;
          final TableItem item = table.getItem(index);
          final Rectangle rect = item.getBounds(column);
          if (rect.contains(pt)) {
            final CCombo combo = new CCombo(table, SWT.READ_ONLY);
            for (int i = 0; i < values.length; i++) {
              combo.add(values[i]);
            }
            final String initialValue = item.getText(column);
            final boolean[] isMouseOverCombo = new boolean[] { false };
            Listener comboListener = new Listener() {
              public void handleEvent(final Event e) {
                Event updateEvent = new Event();
                switch (e.type) {
                  case SWT.FocusOut:
                    if (isMouseOverCombo[0]) return;
                    if (combo.getSelectionIndex() != -1 && !initialValue.equals(combo.getText())) {
                      item.setText(column, combo.getText());
                      updateEvent.item = item;
                      updateEvent.index = column;
                      table.notifyListeners(SWT.SetData, updateEvent);
                    }
                    combo.dispose();
                    break;
                  case SWT.Traverse:
                    switch (e.detail) {
                      case SWT.TRAVERSE_RETURN:
                        if (combo.getSelectionIndex() != -1 && !initialValue.equals(combo.getText())) {
                          item.setText(column, combo.getText());
                          updateEvent.item = item;
                          updateEvent.index = column;
                          table.notifyListeners(SWT.SetData, updateEvent);
                        }
                        combo.dispose();
                        e.doit = false;
                        break;
                      case SWT.TRAVERSE_ESCAPE:
                        combo.dispose();
                        e.doit = false;
                        break;
                    }
                    break;
                }
              }
            };
            combo.addListener(SWT.FocusOut, comboListener);
            combo.addListener(SWT.Traverse, comboListener);
            combo.addListener(SWT.MouseEnter, new Listener() {
              public void handleEvent(Event e) {
                isMouseOverCombo[0] = true;
              }
            });
            combo.addListener(SWT.MouseExit, new Listener() {
              public void handleEvent(Event e) {
                isMouseOverCombo[0] = false;
              }
            });
            combo.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                Event updateEvent = new Event();
                item.setText(column, combo.getText());
                updateEvent.item = item;
                updateEvent.index = column;
                table.notifyListeners(SWT.SetData, updateEvent);
                combo.dispose();
              }
            });
            editor.setEditor(combo, item, column);
            combo.select(combo.indexOf(item.getText(column)));
            combo.setFocus();
            return;
          }
          if (!visible && rect.intersects(clientArea)) {
            visible = true;
          }
          if (!visible) return;
          index++;
        }
      }
    });
  }

  private static class FolderSelectionContentProvider extends WorkbenchContentProvider {
    @Override
    public boolean hasChildren(Object element) {
      Object[] children = getChildren(element);
      for (int i = 0; i < children.length; i++)
        if (children[i] instanceof IFolder) return true;
      return false;
    }
  }

  public static IFolder openSelectFolderDialog(final IProject project, String title, String message) {
    FolderSelectionDialog dialog = new FolderSelectionDialog(TcPlugin.getActiveWorkbenchShell(),
                                                             new WorkbenchLabelProvider(),
                                                             new FolderSelectionContentProvider());

    dialog.setInput(project.getWorkspace());
    dialog.addFilter(new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IProject) return ((IProject) element).equals(project);
        return element instanceof IFolder;
      }
    });
    dialog.setAllowMultiple(false);
    dialog.setTitle(title);
    dialog.setMessage(message);

    dialog.setValidator(new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        String id = TcPlugin.getPluginId();
        if (selection == null || selection.length != 1 || !(selection[0] instanceof IFolder)) { return new Status(
                                                                                                                  IStatus.ERROR,
                                                                                                                  id,
                                                                                                                  IStatus.ERROR,
                                                                                                                  "",
                                                                                                                  null); }
        return new Status(IStatus.OK, id, IStatus.OK, "", null);
      }
    });

    if (dialog.open() == Window.OK) { return (IFolder) dialog.getFirstResult(); }

    return null;
  }
}
