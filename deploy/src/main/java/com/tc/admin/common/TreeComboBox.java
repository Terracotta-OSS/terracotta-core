/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TreeComboBox extends XContainer implements TreeWillExpandListener, TreeModelListener {
  private AbstractButton    triggerButton;
  private TriggerHandler    triggerHandler;
  protected JTree           tree;
  protected TreeModel       treeModel;
  protected TreePath        selectionPath;
  private TreeMouseHandler  treeMouseHandler;
  private SelectionRenderer selectionRenderer;
  private JPopupMenu        popup;

  public TreeComboBox() {
    this(null);
  }

  public TreeComboBox(ActionListener listener) {
    super(new GridBagLayout());

    triggerHandler = new TriggerHandler();
    treeMouseHandler = new TreeMouseHandler();

    setBackground(UIManager.getColor("TextField.background"));
    setForeground(UIManager.getColor("TextField.foreground"));
    setBorder(UIManager.getBorder("TextField.border"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = gbc.gridx = 0;
    gbc.insets = new Insets(0, 1, 0, 1);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    add(selectionRenderer = new SelectionRenderer(), gbc);
    gbc.gridx++;
    selectionRenderer.addMouseListener(triggerHandler);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.anchor = GridBagConstraints.EAST;
    add(triggerButton = createArrowButton(), gbc);
    triggerButton.setRequestFocusEnabled(false);
    triggerButton.resetKeyboardActions();
    triggerButton.setInheritsPopupMenu(true);
    triggerButton.addItemListener(triggerHandler);

    popup = new JPopupMenu();
    popup.addHierarchyListener(triggerHandler);
    popup.setLayout(new BorderLayout());
    tree = createTree();
    tree.setVisibleRowCount(10);
    tree.addTreeWillExpandListener(this);
    popup.add(new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    tree.addMouseListener(treeMouseHandler);
    tree.addMouseMotionListener(treeMouseHandler);
    tree.setSelectionModel(new TreeSelectionModel());

    if (listener != null) {
      this.listenerList.add(ActionListener.class, listener);
    }

    setComponentPopupMenu(createPopupMenu());

    setTreeModel(createTreeModel());
  }

  protected JPopupMenu createPopupMenu() {
    JPopupMenu menu = new JPopupMenu();
    menu.add(new ReshapeAction());
    return menu;
  }

  private class ReshapeAction extends AbstractAction {
    private ReshapeAction() {
      super("Reshape");
    }

    public void actionPerformed(ActionEvent e) {
      treeModelChanged();
    }
  }

  protected JTree createTree() {
    return new JTree();
  }

  protected TreeModel createTreeModel() {
    return new DefaultTreeModel(new DefaultMutableTreeNode("root"));
  }

  private class TreeSelectionModel extends DefaultTreeSelectionModel {
    private TreePath[] filterPaths(TreePath[] paths) {
      if (paths != null) {
        TreePath rootPath = new TreePath(treeModel.getRoot());
        for (int i = 0; i < paths.length; i++) {
          if (!acceptPath(paths[i])) {
            paths[i] = rootPath;
          }
        }
      }
      return paths;
    }

    @Override
    public void setSelectionPaths(TreePath[] paths) {
      super.setSelectionPaths(filterPaths(paths));
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
      super.addSelectionPaths(filterPaths(paths));
    }
  }

  private boolean isSelectedPath(TreePath path) {
    return path != null && path.equals(selectionPath);
  }

  private class TreeMouseHandler extends MouseAdapter implements MouseMotionListener {
    @Override
    public void mouseReleased(MouseEvent e) {
      Point p = e.getPoint();
      int selRow = tree.getRowForLocation(p.x, p.y);
      if (selRow != -1) {
        TreePath path = tree.getPathForRow(selRow);
        if (!isSelectedPath(path)) {
          if (acceptPath(path)) {
            setSelectedPath(path);
            hidePopup();
          }
        } else {
          hidePopup();
        }
      }
    }

    // MouseMotionListener implementation

    private void handleMouseMotion(MouseEvent e) {
      if (true) { return; }
      Point p = e.getPoint();
      int selRow = tree.getRowForLocation(p.x, p.y);
      if (selRow != -1) {
        TreePath path = tree.getPathForRow(selRow);
        if (path != null) {
          tree.setSelectionPath(path);
          tree.scrollPathToVisible(path);
        }
      }
    }

    public void mouseMoved(MouseEvent e) {
      handleMouseMotion(e);
    }

    public void mouseDragged(MouseEvent e) {
      handleMouseMotion(e);
    }
  }

  public void addActionListener(ActionListener l) {
    removeActionListener(l);
    listenerList.add(ActionListener.class, l);
  }

  public void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }

  protected void fireActionPerformed(ActionEvent event) {
    if (listenerList != null) {
      Object[] listeners = listenerList.getListenerList();
      ActionEvent e = null;
      for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ActionListener.class) {
          if (e == null) {
            e = new ActionEvent(TreeComboBox.this, ActionEvent.ACTION_PERFORMED, "ElementSelected",
                                System.currentTimeMillis(), 0);
          }
          ((ActionListener) listeners[i + 1]).actionPerformed(e);
        }
      }
    }
  }

  private class TriggerHandler extends MouseAdapter implements ItemListener, HierarchyListener {
    public void itemStateChanged(ItemEvent e) {
      if (triggerButton.isSelected()) {
        showPopup();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (!popup.isShowing()) {
        showPopup();
      }
    }

    public void hierarchyChanged(HierarchyEvent e) {
      long flags = e.getChangeFlags();
      if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
        boolean isPopupShowing = popup.isShowing();
        if (triggerButton.isSelected() != isPopupShowing) {
          triggerButton.removeItemListener(this);
          triggerButton.setSelected(isPopupShowing);
          triggerButton.addItemListener(this);
        }
      }
    }
  }

  protected void showPopup() {
    Point p = getPopupLocation();
    popup.show(this, p.x, p.y);
    tree.requestFocusInWindow();
  }

  protected void hidePopup() {
    popup.setVisible(false);
  }

  protected boolean acceptPath(TreePath path) {
    return true;
  }

  public TreePath getSelectedPath() {
    return selectionPath;
  }

  public void setSelectedPath(TreePath path) {
    if (!isSelectedPath(path)) {
      tree.setSelectionPath(selectionPath = path);
      fireActionPerformed(null);
      selectionRenderer.repaint();
    }
  }

  public void resetToLastSelectedPath() {
    TreePath path = getSelectedPath();
    if (path != null) {
      tree.setSelectionPath(selectionPath = path);
      fireActionPerformed(null);
      selectionRenderer.repaint();
    }
  }

  public void setSelectedPath(String nodeName) {
    XTreeNode node = ((XTreeNode) treeModel.getRoot()).findNodeByName(nodeName);
    if (node != null) {
      setSelectedPath(new TreePath(node.getPath()));
    }
  }

  public Object getSelectedObject() {
    return selectionPath != null ? selectionPath.getLastPathComponent() : null;
  }

  private class SelectionRenderer extends JComponent {
    @Override
    protected void paintComponent(Graphics g) {
      if (selectionPath != null) {
        Component c = getTreeRendererComponent(selectionPath);
        if (c != null) {
          getCellRendererPane(c, this).paintComponent(g, c, this, 0, 0, getWidth(), getHeight(), true);
        }
      }
    }

    @Override
    public boolean isShowing() {
      return true;
    }
  }

  public Component getTreeRendererComponent(TreePath path) {
    if (tree.isVisible(path)) {
      TreeCellRenderer r = tree.getCellRenderer();
      if (r != null) {
        Object obj = path.getLastPathComponent();
        int row = tree.getRowForPath(path);
        boolean selected = false;
        boolean expanded = true;
        boolean hasFocus = false;
        boolean isLeaf = tree.getModel().isLeaf(obj);
        return r.getTreeCellRendererComponent(tree, obj, selected, expanded, isLeaf, row, hasFocus);
      }
    }
    return null;
  }

  private static CellRendererPane getCellRendererPane(Component c, Container p) {
    Container shell = c.getParent();
    if (shell instanceof CellRendererPane) {
      if (shell.getParent() != p) {
        p.add(shell);
      }
    } else {
      shell = new CellRendererPane();
      shell.add(c);
      p.add(shell);
    }
    return (CellRendererPane) shell;
  }

  private Point getPopupLocation() {
    popup.setPreferredSize(null);
    popup.setMinimumSize(null);
    popup.setMaximumSize(null);

    Dimension popupSize = popup.getPreferredSize();
    popupSize.width = getWidth();

    Rectangle popupBounds = computePopupBounds(0, getBounds().height, popupSize.width, popupSize.height);
    Point popupLocation = popupBounds.getLocation();

    popup.setPopupSize(popupSize);

    return popupLocation;
  }

  protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Rectangle screenBounds;

    GraphicsConfiguration gc = getGraphicsConfiguration();
    Point p = new Point();
    SwingUtilities.convertPointFromScreen(p, this);
    if (gc != null) {
      Insets screenInsets = toolkit.getScreenInsets(gc);
      screenBounds = gc.getBounds();
      screenBounds.width -= (screenInsets.left + screenInsets.right);
      screenBounds.height -= (screenInsets.top + screenInsets.bottom);
      screenBounds.x += (p.x + screenInsets.left);
      screenBounds.y += (p.y + screenInsets.top);
    } else {
      screenBounds = new Rectangle(p, toolkit.getScreenSize());
    }

    Rectangle rect = new Rectangle(px, py, pw, ph);
    if (py + ph > screenBounds.y + screenBounds.height && ph < screenBounds.height) {
      rect.y = -rect.height;
    }
    return rect;
  }

  public void setTreeModel(TreeModel treeModel) {
    tree.getModel().removeTreeModelListener(this);
    tree.setModel(this.treeModel = treeModel);
    treeModel.addTreeModelListener(this);
    treeModelChanged();
    selectInitialPath();
  }

  protected void selectInitialPath() {
    for (int i = 0; i < tree.getRowCount(); i++) {
      TreePath path = tree.getPathForRow(i);
      if (acceptPath(path)) {
        setSelectedPath(path);
        break;
      }
    }
  }

  public void reset() {
    treeModel.removeTreeModelListener(this);
    XTreeNode root = (XTreeNode) treeModel.getRoot();
    root.removeAllChildren();
    root.nodeStructureChanged();
  }

  public void treeWillExpand(TreeExpansionEvent event) {
    /**/
  }

  public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    if (false) { throw new ExpandVetoException(event); }
  }

  protected AbstractButton createArrowButton() {
    AbstractButton button = new BasicToggleArrowButton(SwingConstants.SOUTH,
                                                       UIManager.getColor("ComboBox.buttonBackground"),
                                                       UIManager.getColor("ComboBox.buttonShadow"),
                                                       UIManager.getColor("ComboBox.buttonDarkShadow"),
                                                       UIManager.getColor("ComboBox.buttonHighlight"));
    button.setOpaque(true);
    return button;
  }

  private final XTree sizingTree = new XTree();

  protected void treeModelChanged() {
    sizingTree.setModel(treeModel);
    XTree.expandAll(sizingTree, true);
    tree.setPreferredSize(sizingTree.getPreferredSize());
    popup.setPreferredSize(null);
    Dimension tps = popup.getPreferredSize();
    Dimension srps = selectionRenderer.getPreferredSize();
    srps.width = tps.width;
    srps.height = tree.getRowHeight();
    selectionRenderer.setPreferredSize(srps);
    revalidate();
    repaint();

    if (popup.isVisible()) {
      showPopup();
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public void treeNodesChanged(TreeModelEvent e) {
    treeModelChanged();
  }

  public void treeNodesInserted(TreeModelEvent e) {
    treeModelChanged();
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    treeModelChanged();
  }

  public void treeStructureChanged(TreeModelEvent e) {
    treeModelChanged();
  }

  @Override
  public void tearDown() {
    treeModel.removeTreeModelListener(this);
    tree.removeMouseListener(treeMouseHandler);
    tree.removeMouseMotionListener(treeMouseHandler);

    synchronized (this) {
      triggerButton = null;
      triggerHandler = null;
      tree = null;
      treeModel = null;
      selectionRenderer = null;
      treeMouseHandler = null;
      popup = null;
    }
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("TreeComboBoxTest");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container cp = frame.getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 10, 5, 10);
    TreeComboBox treeComboBox = new TreeComboBox() {
      @Override
      public TreeModel createTreeModel() {
        return tree.getModel();
      }
    };
    cp.add(treeComboBox, gbc);
    frame.setMinimumSize(frame.getPreferredSize());
    frame.pack();
    WindowHelper.center(frame);
    frame.setVisible(true);
  }
}
