/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.ClusterElementChooser;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class ObjectBrowser extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private IAdminClientContext adminClientContext;
  private IClusterModel       clusterModel;
  private ClusterListener     clusterListener;
  private IBasicObject[]      roots;
  private XLabel              currentViewLabel;
  private ElementChooser      elementChooser;
  private PagedView           pagedView;
  private final XContainer    mainPanel;
  private final XContainer    messagePanel;
  private XLabel              messageLabel;
  private boolean             inited;

  private static final String CLUSTER_HEAP_NODE_NAME = "ClusterHeapNode";

  public ObjectBrowser(IAdminClientContext adminClientContext, IClusterModel clusterModel, IBasicObject[] roots) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.roots = roots;

    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      elementChooser.setupTreeModel();
      addNodePanels();
      removeAll();
      add(mainPanel);

    } else {
      add(messagePanel);
      messageLabel.setText(adminClientContext.getString("cluster.not.ready.msg"));
    }
  }

  private XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());

    panel.add(pagedView = new PagedView(), BorderLayout.CENTER);

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.EAST;

    Font headerFont = (Font) adminClientContext.getObject("header.label.font");
    XLabel headerLabel = new XLabel(adminClientContext.getString("current.view.type"));
    topPanel.add(headerLabel, gbc);
    headerLabel.setFont(headerFont);
    gbc.gridx++;

    topPanel.add(currentViewLabel = new XLabel(), gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    headerLabel = new XLabel(adminClientContext.getString("select.view"));
    topPanel.add(headerLabel, gbc);
    headerLabel.setFont(headerFont);
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);
    elementChooser.setSelectedPath(CLUSTER_HEAP_NODE_NAME);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    panel.add(topPanel, BorderLayout.NORTH);

    return panel;
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(adminClientContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) adminClientContext.getObject("message.label.font"));
    return panel;
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, ObjectBrowser.this);
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      XTreeNode aggregateNode = new XTreeNode(adminClientContext.getString("aggregate.view"));
      ComponentNode clusterHeapNode = new ComponentNode(adminClientContext.getString("object.browser.cluster.heap"));
      clusterHeapNode.setName(CLUSTER_HEAP_NODE_NAME);
      aggregateNode.add(clusterHeapNode);
      ClientsNode clientsNode = new ClientsNode(adminClientContext, clusterModel) {
        @Override
        protected void updateLabel() {/**/
        }
      };
      clientsNode.setLabel(adminClientContext.getString("runtime.stats.per.client.view"));
      return new XTreeNode[] { aggregateNode, clientsNode };
    }

    @Override
    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      if (o instanceof XTreeNode) {
        XTreeNode node = (XTreeNode) o;
        return CLUSTER_HEAP_NODE_NAME.equals(node.getName()) || node instanceof ClientNode;
      }
      return false;
    }
  }

  public void setObjects(IBasicObject[] roots) {
    this.roots = roots;
    for (Component comp : pagedView.getComponents()) {
      if (comp instanceof RootsPanel) {
        ((RootsPanel) comp).setObjects(roots);
      }
    }
  }

  public void clearModel() {
    for (Component comp : pagedView.getComponents()) {
      if (comp instanceof RootsPanel) {
        ((RootsPanel) comp).clearModel();
      }
    }
  }

  public void add(IBasicObject root) {
    ArrayList<IBasicObject> list = new ArrayList<IBasicObject>(Arrays.asList(roots));
    list.add(root);
    roots = list.toArray(new IBasicObject[list.size()]);
    for (Component comp : pagedView.getComponents()) {
      if (comp instanceof RootsPanel) {
        ((RootsPanel) comp).add(root);
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (pagedView.hasPage(name)) {
      pagedView.setPage(name);
    }
    TreePath path = elementChooser.getSelectedPath();
    Object type = path.getPathComponent(1);
    currentViewLabel.setText(type.toString());
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      removeAll();
      if (clusterModel.isReady()) {
        if (!inited) {
          elementChooser.setupTreeModel();
          addNodePanels();
        }
        add(mainPanel);
      } else {
        messageLabel.setText(adminClientContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
      }
      revalidate();
      repaint();
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(ObjectBrowser.this);
      }
      if (newActive != null) {
        newActive.removeClientConnectionListener(ObjectBrowser.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      if (adminClientContext != null) {
        adminClientContext.log(e);
      } else {
        super.handleUncaughtError(e);
      }
    }
  }

  public void clientConnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        pagedView.addPage(createClientHeapPanel(client));
      }
    });
  }

  public void clientDisconnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Component page = pagedView.getPage(client.toString());
        if (page != null) {
          pagedView.remove(page);
        }
      }
    });
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    }
  }

  private void addNodePanels() {
    pagedView.removeAll();
    pagedView.addPage(createClusterHeapPanel());
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      for (IClient client : activeCoord.getClients()) {
        pagedView.addPage(createClientHeapPanel(client));
      }
      activeCoord.addClientConnectionListener(this);
    }
    elementChooser.setSelectedPath(CLUSTER_HEAP_NODE_NAME);
    pagedView.addPropertyChangeListener(this);
    inited = true;
  }

  private RootsPanel createClusterHeapPanel() {
    RootsPanel panel = new RootsPanel(adminClientContext, clusterModel, clusterModel, roots);
    panel.setName(CLUSTER_HEAP_NODE_NAME);
    return panel;
  }

  private RootsPanel createClientHeapPanel(IClient client) {
    RootsPanel panel = new RootsPanel(adminClientContext, clusterModel, client, client, roots);
    panel.setExplainationText(adminClientContext.getMessage("resident.object.message"));
    panel.setName(client.toString());
    return panel;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    pagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      roots = null;
      elementChooser = null;
      pagedView = null;
    }

    super.tearDown();
  }

}
