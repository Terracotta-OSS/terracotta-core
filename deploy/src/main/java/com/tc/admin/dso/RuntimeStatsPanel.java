/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.AggregateServerRuntimeStatsPanel;
import com.tc.admin.ClusterElementChooser;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.ServerGroupsNode;
import com.tc.admin.ServerNode;
import com.tc.admin.ServerRuntimeStatsPanel;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class RuntimeStatsPanel extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private final IAdminClientContext adminClientContext;
  private final IClusterModel       clusterModel;
  private final ClusterListener     clusterListener;

  private XLabel                    currentViewLabel;
  private ElementChooser            elementChooser;
  private PagedView                 pagedView;
  private final XContainer          mainPanel;
  private final XContainer          messagePanel;
  private XLabel                    messageLabel;

  private static final String       AGGREGATE_SERVER_STATS_NODE_NAME = "AggregateServerStatsNode";

  public final static Icon          WAIT_ICON                        = new ImageIcon(
                                                                                     RuntimeStatsPanel.class
                                                                                         .getResource("/com/tc/admin/icons/wait.gif"));

  public RuntimeStatsPanel(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
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
    elementChooser.setSelectedPath(AGGREGATE_SERVER_STATS_NODE_NAME);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    panel.add(topPanel, BorderLayout.NORTH);

    return panel;
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(adminClientContext.getString("cluster.not.ready.msg"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) adminClientContext.getObject("message.label.font"));
    messageLabel.setIcon(WAIT_ICON);
    messageLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (true) {
          Properties props = System.getProperties();
          if (props != null) {
            /**/
          }
        }
      }
    });
    return panel;
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, RuntimeStatsPanel.this);
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      XTreeNode aggregateViewsNode = new XTreeNode(adminClientContext.getString("aggregate.view"));
      ComponentNode aggregateServerStatsNode = new ComponentNode(
                                                                 adminClientContext
                                                                     .getString("runtime.stats.aggregate.server.stats"));
      aggregateServerStatsNode.setName(AGGREGATE_SERVER_STATS_NODE_NAME);
      aggregateViewsNode.add(aggregateServerStatsNode);
      ClientsNode clientsNode = new ClientsNode(adminClientContext, clusterModel) {
        @Override
        protected void updateLabel() {/**/
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
        return AGGREGATE_SERVER_STATS_NODE_NAME.equals(node.getName()) || node instanceof ClientNode
               || node instanceof ServerNode;
      }
      return false;
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (!pagedView.hasPage(name)) {
      if (node instanceof ServerNode) {
        IServer server = ((ServerNode) node).getServer();
        pagedView.addPage(createServerViewPanel(server));

      } else if (node instanceof ClientNode) {
        IClient client = ((ClientNode) node).getClient();
        pagedView.addPage(createClientViewPanel(client));
      }
    }
    pagedView.setPage(name);
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
      if (clusterModel.isReady()) {
        init();
      } else {
        suspend();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(RuntimeStatsPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(RuntimeStatsPanel.this);
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

  private void init() {
    removeAll();
    elementChooser.setupTreeModel();
    addNodePanels();
    add(mainPanel);
  }

  private void suspend() {
    removeAll();
    add(messagePanel);
  }

  public void clientConnected(final IClient client) {
    /**/
  }

  public void clientDisconnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ClientRuntimeStatsPanel panel = (ClientRuntimeStatsPanel) pagedView.getPage(client.toString());
        if (panel != null) {
          pagedView.remove(panel);
          panel.tearDown();
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

  private void clearPagedView() {
    for (Component comp : pagedView.getComponents()) {
      if (comp instanceof BaseRuntimeStatsPanel) {
        ((BaseRuntimeStatsPanel) comp).tearDown();
      }
    }
    pagedView.removeAll();
  }

  private void addNodePanels() {
    clearPagedView();
    pagedView.addPage(createAggregateServerStatsPanel());
    for (IServerGroup group : clusterModel.getServerGroups()) {
      for (IServer server : group.getMembers()) {
        if (server.isActiveCoordinator()) {
          server.addClientConnectionListener(this);
        }
      }
    }
    elementChooser.setSelectedPath(AGGREGATE_SERVER_STATS_NODE_NAME);
    pagedView.addPropertyChangeListener(this);
  }

  private AggregateServerRuntimeStatsPanel createAggregateServerStatsPanel() {
    AggregateServerRuntimeStatsPanel panel = new AggregateServerRuntimeStatsPanel(adminClientContext, clusterModel);
    panel.setName(AGGREGATE_SERVER_STATS_NODE_NAME);
    panel.startMonitoringRuntimeStats();
    return panel;
  }

  private ServerRuntimeStatsPanel createServerViewPanel(IServer server) {
    ServerRuntimeStatsPanel panel = new ServerRuntimeStatsPanel(adminClientContext, server);
    panel.setName(server.toString());
    panel.startMonitoringRuntimeStats();
    return panel;
  }

  private ClientRuntimeStatsPanel createClientViewPanel(IClient client) {
    ClientRuntimeStatsPanel panel = new ClientRuntimeStatsPanel(adminClientContext, client);
    panel.setName(client.toString());
    panel.startMonitoringRuntimeStats();
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

    clearPagedView();

    super.tearDown();
  }
}
