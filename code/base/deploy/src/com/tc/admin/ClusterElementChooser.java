/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.CellRendererPane;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

public abstract class ClusterElementChooser extends XContainer implements TreeWillExpandListener, TreeModelListener {
  private IClusterModel     clusterModel;
  private ClusterListener   clusterListener;
  private JButton           triggerButton;
  private MouseListener     triggerHandler;
  private XTree             tree;
  private XTreeModel        treeModel;
  private TreePath          selectionPath;
  private SelectionRenderer selectionRenderer;
  private CellRendererPane  cellRendererPane;
  private XScrollPane       scroller;
  private JPopupMenu        popup;
  private boolean           inited;
  private EventListenerList listenerList;

  public ClusterElementChooser(IClusterModel clusterModel, ActionListener listener) {
    super(new GridBagLayout());

    triggerHandler = new TriggerButtonHandler();

    setBackground(UIManager.getColor("TextField.background"));
    setBorder(UIManager.getBorder("TextField.border"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = gbc.gridx = 0;
    gbc.insets = new Insets(0, 1, 0, 0);

    cellRendererPane = new CellRendererPane();
    add(cellRendererPane);

    add(selectionRenderer = new SelectionRenderer(), gbc);
    gbc.gridx++;
    selectionRenderer.addMouseListener(triggerHandler);

    add(triggerButton = createArrowButton(), gbc);
    triggerButton.setRequestFocusEnabled(false);
    triggerButton.resetKeyboardActions();
    triggerButton.setInheritsPopupMenu(true);
    triggerButton.addMouseListener(triggerHandler);

    popup = new JPopupMenu();
    popup.setLayout(new BorderLayout());
    tree = new XTree();
    tree.addTreeWillExpandListener(this);
    tree.setModel(treeModel = new XTreeModel());
    treeModel.addTreeModelListener(this);
    popup.add(scroller = new XScrollPane(tree));
    tree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
          TreePath path = tree.getSelectionPath();
          if (path != null && acceptPath(path)) {
            setSelectedPath(path);
            popup.setVisible(false);
          }
        }
      }
    });

    this.clusterModel = clusterModel;
    this.listenerList = new EventListenerList();
    if (listener != null) {
      this.listenerList.add(ActionListener.class, listener);
    }

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      setupTreeModel();
    }
  }

  public void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  public void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }

  protected void fireActionPerformed(ActionEvent event) {
    Object[] listeners = listenerList.getListenerList();
    ActionEvent e = null;
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ActionListener.class) {
        if (e == null) {
          e = new ActionEvent(ClusterElementChooser.this, ActionEvent.ACTION_PERFORMED, "ClusterElementSelected",
                              System.currentTimeMillis(), 0);
        }
        ((ActionListener) listeners[i + 1]).actionPerformed(e);
      }
    }
  }

  private class TriggerButtonHandler extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
      togglePopup();
    }
  }

  protected void togglePopup() {
    boolean isShowing = popup.isVisible();
    if (isShowing) {
      hidePopup();
    } else {
      showPopup();
    }
  }

  protected void showPopup() {
    Point p = getPopupLocation();
    popup.show(this, p.x, p.y);
  }

  protected void hidePopup() {
    popup.setVisible(false);
  }

  protected boolean acceptPath(TreePath path) {
    Object o = path.getLastPathComponent();
    return (o instanceof ClusterElementNode);
  }

  public TreePath getSelectedPath() {
    return selectionPath;
  }

  public void setSelectedPath(TreePath path) {
    selectionPath = path;
    fireActionPerformed(null);
    revalidate();
    repaint();
  }

  public void setSelectedPath(String nodeName) {
    XTreeNode node = ((XRootNode) treeModel.getRoot()).findNodeByName(nodeName);
    if (node != null) {
      setSelectedPath(new TreePath(node.getPath()));
    }
  }

  public Object getSelectedObject() {
    return selectionPath != null ? selectionPath.getLastPathComponent() : null;
  }

  private class SelectionRenderer extends JComponent {
    protected void paintComponent(Graphics g) {
      if (selectionPath != null) {
        Component c = tree.getRendererComponent(selectionPath);
        if (c != null) {
          cellRendererPane.paintComponent(g, c, this, 0, 0, getWidth(), getHeight(), true);
        }
      }
    }

    public boolean isShowing() {
      return true;
    }
  }

  private Point getPopupLocation() {
    Dimension popupSize = popup.getPreferredSize();
    popupSize.height = 200;

    int x = getBounds().width - popupSize.width;
    Rectangle popupBounds = computePopupBounds(x, getBounds().height, popupSize.width, popupSize.height);
    Dimension scrollSize = popupBounds.getSize();
    Point popupLocation = popupBounds.getLocation();

    scroller.setMaximumSize(scrollSize);
    scroller.setPreferredSize(scrollSize);
    scroller.setMinimumSize(scrollSize);

    tree.revalidate();

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

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    protected void handleReady() {
      if (!inited && clusterModel.isReady()) {
        setupTreeModel();
      }
    }
  }

  protected abstract XTreeNode[] createTopLevelNodes();

  private void setupTreeModel() {
    XRootNode root = (XRootNode) treeModel.getRoot();
    for (XTreeNode child : createTopLevelNodes()) {
      root.add(child);
    }
    root.nodeStructureChanged();
    setSelectedPath(new TreePath(((XTreeNode) root.getFirstChild()).getPath()));
    inited = true;
  }

  public TreePath getPath(IClusterModelElement clusterElement) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    Enumeration e = root.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
      if (node instanceof ClusterElementNode) {
        IClusterModelElement cme = ((ClusterElementNode) node).getClusterElement();
        if (clusterElement.equals(cme)) { return new TreePath(node.getPath()); }
      }
    }
    return null;
  }

  public void treeWillExpand(TreeExpansionEvent event) {
    /**/
  }

  public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    throw new ExpandVetoException(event);
  }

  protected JButton createArrowButton() {
    JButton button = new BasicArrowButton(SwingConstants.SOUTH, UIManager.getColor("ComboBox.buttonBackground"),
                                          UIManager.getColor("ComboBox.buttonShadow"), UIManager
                                              .getColor("ComboBox.buttonDarkShadow"), UIManager
                                              .getColor("ComboBox.buttonHighlight"));
    return button;
  }

  private void treeModelChanged() {
    tree.expandAll();
    selectionRenderer.setPreferredSize(tree.getMaxItemSize());
    revalidate();
    repaint();
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

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    treeModel.removeTreeModelListener(this);

    synchronized (this) {
      clusterModel = null;
      clusterListener = null;
      triggerButton = null;
      tree = null;
      treeModel = null;
      selectionRenderer = null;
      popup = null;
    }
  }
}
