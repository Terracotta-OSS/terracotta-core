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
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class ObjectBrowser extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private IAdminClientContext adminClientContext;
  private IClusterModel       clusterModel;
  private ClusterListener     clusterListener;
  private IBasicObject[]      roots;
  private ElementChooser      elementChooser;
  private PagedView           pagedView;
  private boolean             inited;

  private static final String CLUSTER_HEAP_NODE_NAME = "ClusterHeapNode";
  private static final String EMPTY_PAGE             = "EmptyPage";

  public ObjectBrowser(IAdminClientContext adminClientContext, IClusterModel clusterModel, IBasicObject[] roots) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.roots = roots;

    add(pagedView = new PagedView(), BorderLayout.CENTER);

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.EAST;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    topPanel.add(new XLabel("Filter by:"), gbc);
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    add(topPanel, BorderLayout.NORTH);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      addNodePanels();
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, ObjectBrowser.this);
    }

    protected XTreeNode[] createTopLevelNodes() {
      ComponentNode clusterHeapNode = new ComponentNode("Cluster Heap");
      clusterHeapNode.setName(CLUSTER_HEAP_NODE_NAME);
      ClientsNode clientsNode = new ClientsNode(adminClientContext, clusterModel);
      clientsNode.setLabel("Client View");
      return new XTreeNode[] { clusterHeapNode, clientsNode };
    }

    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      return !(o instanceof ClientsNode);
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
    } else {
      pagedView.setPage(EMPTY_PAGE);
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    protected void handleReady() {
      if (!inited && clusterModel.isReady()) {
        addNodePanels();
      }
    }

    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(ObjectBrowser.this);
      }
      if (newActive != null) {
        newActive.removeClientConnectionListener(ObjectBrowser.this);
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
        pagedView.remove(pagedView.getPage(client.toString()));
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
    XLabel emptyPage = new XLabel();
    emptyPage.setName(EMPTY_PAGE);
    pagedView.addPage(emptyPage);

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

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    pagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      roots = null;
      elementChooser.tearDown();
      elementChooser = null;
      pagedView = null;
    }
  }

}
