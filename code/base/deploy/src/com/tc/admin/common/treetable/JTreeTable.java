package com.tc.admin.common.treetable;

import com.tc.admin.common.XTable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

public class JTreeTable extends XTable {
  protected TreeTableModel        treeTableModel;
  protected TreeTableCellRenderer tree;

  public JTreeTable(TreeTableModel treeTableModel) {
    super();

    tree = new TreeTableCellRenderer();

    // Force the JTable and JTree to share their row selection models.
    tree.setSelectionModel(new DefaultTreeSelectionModel() {
      {
        setSelectionModel(listSelectionModel);
        setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      }
    });

    tree.setRowHeight(getRowHeight());
    setDefaultRenderer(TreeTableModel.class, tree);
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

    setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));

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

  /*
   * Workaround for BasicTableUI anomaly. Make sure the UI never tries to paint the editor. The UI currently uses
   * different techniques to paint the renderers and editors and overriding setBounds() below is not the right thing to
   * do for an editor. Returning -1 for the editing row in this case, ensures the editor is never painted.
   */
  public int getEditingRow() {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
  }

  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {

    protected int visibleRow;

    public TreeTableCellRenderer() {
      super();
    }

    public TreeTableCellRenderer(TreeModel model) {
      super(model);
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
  }

}
