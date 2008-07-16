package com.tc.admin.common.treetable;

import com.tc.admin.common.XTable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class JTreeTable extends XTable {
  protected TreeTableModel        treeTableModel;
  protected TreeTableCellRenderer tree;

  public JTreeTable() {
    super();

    tree = new TreeTableCellRenderer();

    // Force the JTable and JTree to share their row selection models.
    ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
    tree.setSelectionModel(selectionWrapper);
    setSelectionModel(selectionWrapper.getListSelectionModel());

    tree.setRowHeight(getRowHeight());
    setDefaultRenderer(TreeTableModel.class, tree);
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

    setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));
  }

  public JTreeTable(TreeTableModel treeTableModel) {
    this();
    setTreeTableModel(treeTableModel);
  }

  public void setTreeTableModel(TreeTableModel model) {
    tree.setModel(this.treeTableModel = model);
    tree.setRootVisible(model.isRootVisible());
    tree.setShowsRootHandles(!model.isRootVisible());

    super.setModel(new TreeTableModelAdapter(model, tree));
  }

  public TreeTableModel getTreeTableModel() {
    return this.treeTableModel;
  }

  /**
   * Overridden to message super and forward the method to the tree. Since the tree is not actually in the component
   * hieachy it will never receive this unless we forward it in this manner.
   */
  public void updateUI() {
    super.updateUI();
    if (tree != null) {
      tree.updateUI();
    }
    // Use the tree's default foreground and background colors in the
    // table.
    LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
  }

  /*
   * Workaround for BasicTableUI anomaly. Make sure the UI never tries to paint the editor. The UI currently uses
   * different techniques to paint the renderers and editors and overriding setBounds() below is not the right thing to
   * do for an editor. Returning -1 for the editing row in this case, ensures the editor is never painted.
   */
  public int getEditingRow() {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
  }

  /**
   * Overridden to pass the new rowHeight to the tree.
   */
  public void setRowHeight(int rowHeight) {
    super.setRowHeight(rowHeight);
    if (tree != null && tree.getRowHeight() != rowHeight) {
      tree.setRowHeight(getRowHeight());
    }
  }

  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {

    protected int visibleRow;

    public TreeTableCellRenderer() {
      super();
    }

    public TreeTableCellRenderer(TreeModel model) {
      super(model);
    }

    /**
     * updateUI is overridden to set the colors of the Tree's renderer to match that of the table.
     */
    public void updateUI() {
      super.updateUI();
      // Make the tree's cell renderer use the table's cell selection
      // colors.
      TreeCellRenderer tcr = getCellRenderer();
      if (tcr instanceof DefaultTreeCellRenderer) {
        DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
        // For 1.1 uncomment this, 1.2 has a bug that will cause an
        // exception to be thrown if the border selection color is
        // null.
        // dtcr.setBorderSelectionColor(null);
        dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
        dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
      }
    }

    /**
     * Sets the row height of the tree, and forwards the row height to the table.
     */
    public void setRowHeight(int rowHeight) {
      if (rowHeight > 0) {
        super.setRowHeight(rowHeight);
        if (JTreeTable.this != null && JTreeTable.this.getRowHeight() != rowHeight) {
          JTreeTable.this.setRowHeight(getRowHeight());
        }
      }
    }

    public void setBounds(int x, int y, int w, int h) {
      super.setBounds(x, 0, w, JTreeTable.this.getHeight());
    }

    public void paint(Graphics g) {
      if (visibleRow == -1) return;
      g.translate(0, -visibleRow * getRowHeight());
      super.paint(g);
      visibleRow = -1;
    }

    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      d.height = getRowHeight();
      return d;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      if (isSelected) setBackground(table.getSelectionBackground());
      else setBackground(table.getBackground());
      visibleRow = row;
      setFont(table.getFont());
      return this;
    }
  }

  public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
      return tree;
    }

    /**
     * Overridden to return false, and if the event is a mouse event it is forwarded to the tree.
     * <p>
     * The behavior for this is debatable, and should really be offered as a property. By returning false, all keyboard
     * actions are implemented in terms of the table. By returning true, the tree would get a chance to do something
     * with the keyboard events. For the most part this is ok. But for certain keys, such as left/right, the tree will
     * expand/collapse where as the table focus should really move to a different column. Page up/down should also be
     * implemented in terms of the table. By returning false this also has the added benefit that clicking outside of
     * the bounds of the tree node, but still in the tree column will select the row, whereas if this returned true that
     * wouldn't be the case.
     * <p>
     * By returning false we are also enforcing the policy that the tree will never be editable (at least by a key
     * sequence).
     */
    public boolean isCellEditable(EventObject e) {
      if (e instanceof MouseEvent) {
        for (int counter = getColumnCount() - 1; counter >= 0; counter--) {
          if (getColumnClass(counter) == TreeTableModel.class) {
            MouseEvent me = (MouseEvent) e;
            int newX = me.getX() - getCellRect(0, counter, true).x;
            final MouseEvent newME = new MouseEvent(tree, me.getID(), me.getWhen(), me.getModifiers(), newX, me.getY(),
                                                    me.getClickCount(), me.isPopupTrigger());
            final MouseEvent releaseME = new MouseEvent(tree, MouseEvent.MOUSE_RELEASED, me.getWhen(), me
                .getModifiers(), newX, me.getY(), me.getClickCount(), me.isPopupTrigger());
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                tree.dispatchEvent(newME);
                tree.dispatchEvent(releaseME);
              }
            });
            break;
          }
        }
      }
      return false;
    }
  }

  /**
   * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to listen for changes in the ListSelectionModel
   * it maintains. Once a change in the ListSelectionModel happens, the paths are updated in the
   * DefaultTreeSelectionModel.
   */
  class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
    /** Set to true when we are updating the ListSelectionModel. */
    protected boolean updatingListSelectionModel;

    public ListToTreeSelectionModelWrapper() {
      super();
      getListSelectionModel().addListSelectionListener(createListSelectionListener());
    }

    /**
     * Returns the list selection model. ListToTreeSelectionModelWrapper listens for changes to this model and updates
     * the selected paths accordingly.
     */
    ListSelectionModel getListSelectionModel() {
      return listSelectionModel;
    }

    /**
     * This is overridden to set <code>updatingListSelectionModel</code> and message super. This is the only place
     * DefaultTreeSelectionModel alters the ListSelectionModel.
     */
    public void resetRowSelection() {
      if (!updatingListSelectionModel) {
        updatingListSelectionModel = true;
        try {
          super.resetRowSelection();
        } finally {
          updatingListSelectionModel = false;
        }
      }
      // Notice how we don't message super if
      // updatingListSelectionModel is true. If
      // updatingListSelectionModel is true, it implies the
      // ListSelectionModel has already been updated and the
      // paths are the only thing that needs to be updated.
    }

    /**
     * Creates and returns an instance of ListSelectionHandler.
     */
    protected ListSelectionListener createListSelectionListener() {
      return new ListSelectionHandler();
    }

    /**
     * If <code>updatingListSelectionModel</code> is false, this will reset the selected paths from the selected rows in
     * the list selection model.
     */
    protected void updateSelectedPathsFromSelectedRows() {
      if (!updatingListSelectionModel) {
        updatingListSelectionModel = true;
        try {
          // This is way expensive, ListSelectionModel needs an
          // enumerator for iterating.
          int min = listSelectionModel.getMinSelectionIndex();
          int max = listSelectionModel.getMaxSelectionIndex();

          clearSelection();
          if (min != -1 && max != -1) {
            for (int counter = min; counter <= max; counter++) {
              if (listSelectionModel.isSelectedIndex(counter)) {
                TreePath selPath = tree.getPathForRow(counter);

                if (selPath != null) {
                  addSelectionPath(selPath);
                }
              }
            }
          }
        } finally {
          updatingListSelectionModel = false;
        }
      }
    }

    /**
     * Class responsible for calling updateSelectedPathsFromSelectedRows when the selection of the list changse.
     */
    class ListSelectionHandler implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        updateSelectedPathsFromSelectedRows();
      }
    }
  }

}
