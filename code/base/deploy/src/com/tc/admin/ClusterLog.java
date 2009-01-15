/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class ClusterLog extends XContainer implements ActionListener {
  private IAdminClientContext adminClientContext;
  private IClusterModel       clusterModel;
  private ClusterListener     clusterListener;
  private ElementChooser      elementChooser;
  private PagedView           pagedView;
  private boolean             inited;

  private static final String EMPTY_PAGE = "EmptyPage";

  public ClusterLog(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

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

    topPanel.add(new XLabel("View log for:"), gbc);
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);
    elementChooser.addActionListener(this);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    add(topPanel, BorderLayout.NORTH);

    setName(clusterModel.toString());

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      addNodePanels();
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, ClusterLog.this);
    }

    protected XTreeNode[] createTopLevelNodes() {
      return new XTreeNode[] { new ServerGroupsNode(adminClientContext, clusterModel) };
    }

    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      return o instanceof ServerNode;
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
  }
  private void addNodePanels() {
    pagedView.removeAll();
    XLabel emptyPage = new XLabel();
    emptyPage.setName(EMPTY_PAGE);
    pagedView.addPage(emptyPage);

    for (IServerGroup group : clusterModel.getServerGroups()) {
      for (IServer server : group.getMembers()) {
        pagedView.addPage(createServerLog(server));
      }
    }
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if(activeCoord != null) {
      elementChooser.setSelectedPath(activeCoord.toString());
    }
    inited = true;
  }

  private JScrollPane createServerLog(IServer server) {
    final ServerLog serverLog = new ServerLog(adminClientContext, server);
    final JScrollPane scroller = new JScrollPane(serverLog, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    JScrollBar scrollBar = scroller.getVerticalScrollBar();
    scrollBar.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        serverLog.setAutoScroll(false);
      }

      public void mouseReleased(MouseEvent e) {
        boolean autoScroll = shouldAutoScroll(scroller, serverLog);
        serverLog.setAutoScroll(autoScroll);
        if (!autoScroll) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              serverLog.requestFocusInWindow();
            }
          });
        }
      }
    });
    scroller.addMouseWheelListener(new MouseWheelListener() {
      public void mouseWheelMoved(MouseWheelEvent e) {
        boolean autoScroll = shouldAutoScroll(scroller, serverLog);
        serverLog.setAutoScroll(autoScroll);
        if (!autoScroll) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              serverLog.requestFocusInWindow();
            }
          });
        }
      }
    });
    serverLog.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_ENTER) {
          serverLog.setAutoScroll(true);
        }
      }
    });
    scroller.setName(server.toString());

    return scroller;
  }

  private boolean shouldAutoScroll(JScrollPane scroller, Component log) {
    JViewport viewport = scroller.getViewport();
    Rectangle visibleRect = viewport.getViewRect();
    Rectangle bounds = log.getBounds();
    return (bounds.y + bounds.height) <= visibleRect.height;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    elementChooser.removeActionListener(this);

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      elementChooser.tearDown();
      elementChooser = null;
      pagedView = null;
    }
  }
}
