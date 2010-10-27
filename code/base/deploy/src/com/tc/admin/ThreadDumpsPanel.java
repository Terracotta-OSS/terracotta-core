/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IClusterNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;

public class ThreadDumpsPanel extends XContainer implements ActionListener, PropertyChangeListener {
  private IAdminClientContext       adminClientContext;
  private IClusterModel             clusterModel;
  private ClusterThreadDumpProvider threadDumpProvider;
  private ClusterListener           clusterListener;
  private ElementChooser            elementChooser;
  private PagedView                 pagedView;
  private boolean                   inited;

  private static final String       ALL_NODES_NODE_NAME = "AllNodesNode";

  public ThreadDumpsPanel(IAdminClientContext adminClientContext, IClusterModel clusterModel,
                          ClusterThreadDumpProvider threadDumpProvider) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.threadDumpProvider = threadDumpProvider;

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

    XLabel label = new XLabel(adminClientContext.getString("take.thread.dump.for"));
    topPanel.add(label, gbc);
    label.setFont((Font) adminClientContext.getObject("header.label.font"));
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);
    elementChooser.setSelectedPath(ALL_NODES_NODE_NAME);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    add(topPanel, BorderLayout.NORTH);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isConnected()) {
      elementChooser.setupTreeModel();
      addNodePanels();
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, ThreadDumpsPanel.this);
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      XTreeNode aggregateViewsNode = new XTreeNode(adminClientContext.getString("aggregate.view"));
      ComponentNode clusterDumpNode = new ComponentNode("Cluster Dump");
      clusterDumpNode.setName(ALL_NODES_NODE_NAME);
      aggregateViewsNode.add(clusterDumpNode);
      ClientsNode clientsNode = new ClientsNode(adminClientContext, clusterModel) {
        @Override
        protected void updateLabel() {
          /**/
        }
      };
      clientsNode.setLabel(adminClientContext.getString("runtime.stats.per.client.view"));
      ServerGroupsNode serverGroupsNode = new ServerGroupsNode(adminClientContext, clusterModel);
      serverGroupsNode.setLabel(adminClientContext.getString("runtime.stats.per.server.view"));
      return new XTreeNode[] { aggregateViewsNode, clientsNode, serverGroupsNode };
    }

    @Override
    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      if (o instanceof XTreeNode) {
        XTreeNode node = (XTreeNode) o;
        return ALL_NODES_NODE_NAME.equals(node.getName()) || node instanceof ClientNode || node instanceof ServerNode;
      }
      return false;
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
      setSelectedPath(ALL_NODES_NODE_NAME);
      treeModelChanged();
    }
  }

  public void actionPerformed(ActionEvent e) {
    pagedView.setPage(ALL_NODES_NODE_NAME);
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (!inited && clusterModel.isConnected()) {
        elementChooser.setupTreeModel();
        addNodePanels();
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

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    }
  }

  private void addNodePanels() {
    pagedView.removeAll();
    pagedView.addPage(createAllNodesPanel());
    elementChooser.setSelectedPath(ALL_NODES_NODE_NAME);
    pagedView.addPropertyChangeListener(this);
    inited = true;
  }

  private ClusterThreadDumpsPanel createAllNodesPanel() {
    ClusterThreadDumpsPanel panel = new ClusterThreadDumpsPanel(adminClientContext, threadDumpProvider) {
      @Override
      public ClusterThreadDumpEntry newEntry() {
        Object selectedObj = elementChooser.getSelectedObject();
        if (selectedObj instanceof ClusterElementNode) {
          ClusterElementNode node = (ClusterElementNode) selectedObj;
          IClusterModelElement clusterElement = node.getClusterElement();
          if (clusterElement instanceof IClusterNode) {
            IClusterNode clusterNode = (IClusterNode) clusterElement;
            ClusterThreadDumpEntry entry = new ClusterThreadDumpEntry(adminClientContext);
            entry.add(clusterNode.toString(), clusterModel.takeThreadDump(clusterNode));
            return entry;
          }
        }
        return super.newEntry();
      }
    };
    panel.setName(ALL_NODES_NODE_NAME);
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
      threadDumpProvider = null;
      clusterListener = null;
      elementChooser = null;
      pagedView = null;
    }

    super.tearDown();
  }
}
