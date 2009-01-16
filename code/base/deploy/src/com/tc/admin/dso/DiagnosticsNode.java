/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.ClusterNode;
import com.tc.admin.ClusterThreadDumpProvider;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.StatsRecorderNode;
import com.tc.admin.ThreadDumpsNode;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.dso.locks.LocksNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.Icon;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class DiagnosticsNode extends ComponentNode implements HyperlinkListener {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected ClusterListener     clusterListener;
  protected XScrollPane         diagnosticsPanel;
  private StatsRecorderNode     statsRecorderNode;
  private int                   statsRecorderNodeIndex;

  public DiagnosticsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel, ClusterNode clusterNode) {
    super(adminClientContext.getString("dso.diagnostics"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(createRuntimeStatsNode());
    try {
      add(createLocksNode(clusterNode));
    } catch (Throwable t) {
      t.printStackTrace();
      // Need a more specific exception but this means we're trying to connect to an
      // older version of the server, that doesn't have the LockMonitorMBean we expect.
    }
    add(createThreadDumpsNode(clusterNode));
    statsRecorderNodeIndex = getChildCount();
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      setStatsRecorderAvailable(activeCoord.isClusterStatsSupported());
    } else {
      clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    public ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      setStatsRecorderAvailable(newActive != null && newActive.isClusterStatsSupported());
    }
  }

  protected RuntimeStatsNode createRuntimeStatsNode() {
    return new RuntimeStatsNode(adminClientContext, clusterModel);
  }

  protected LocksNode createLocksNode(ClusterNode clusterNode) {
    return new LocksNode(adminClientContext, clusterNode);
  }

  protected ThreadDumpsNode createThreadDumpsNode(ClusterThreadDumpProvider threadDumpProvider) {
    return new ThreadDumpsNode(adminClientContext, clusterModel, threadDumpProvider);
  }

  protected StatsRecorderNode createStatsRecorderNode() {
    return new StatsRecorderNode(adminClientContext, getClusterModel());
  }

  void setStatsRecorderAvailable(boolean showStatsRecorderNode) {
    if (showStatsRecorderNode && statsRecorderNode == null) {
      statsRecorderNode = createStatsRecorderNode();
      insertChild(statsRecorderNode, statsRecorderNodeIndex);
    } else if (!showStatsRecorderNode && statsRecorderNode != null) {
      removeChild(statsRecorderNode);
      statsRecorderNode.tearDown();
      statsRecorderNode = null;
    }
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  public Component getComponent() {
    if (diagnosticsPanel == null) {
      XTextPane textPane = new XTextPane();
      diagnosticsPanel = new XScrollPane(textPane);
      try {
        textPane.setPage(getClass().getResource("DiagnosticsIntro.html"));
      } catch (Exception e) {
        e.printStackTrace();
      }
      textPane.setEditable(false);
      textPane.addHyperlinkListener(this);
    }
    return diagnosticsPanel;
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    XTextPane textPane = (XTextPane) e.getSource();
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

    if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet anchor = (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
      String action = (String) anchor.getAttribute(HTML.Attribute.HREF);
      adminClientContext.getAdminClientController().selectNode(this, action);
    }
  }

  public Icon getIcon() {
    return DSOHelper.getHelper().getDiagnosticsIcon();
  }

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);

    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      diagnosticsPanel = null;
    }
  }
}
