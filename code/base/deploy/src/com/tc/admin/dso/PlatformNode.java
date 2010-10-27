/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ClusterNode;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.Icon;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class PlatformNode extends ComponentNode implements HyperlinkListener {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected XScrollPane         diagnosticsPanel;

  public PlatformNode(ClusterNode clusterNode, IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("dso.platform"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(createClusteredHeapNode());
    add(createDiagnosticsNode(clusterNode));
  }

  protected ClusteredHeapNode createClusteredHeapNode() {
    return new ClusteredHeapNode(adminClientContext, getClusterModel());
  }

  protected DiagnosticsNode createDiagnosticsNode(ClusterNode clusterNode) {
    return new DiagnosticsNode(adminClientContext, getClusterModel(), clusterNode);
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public Component getComponent() {
    if (diagnosticsPanel == null) {
      XTextPane textPane = new XTextPane();
      diagnosticsPanel = new XScrollPane(textPane);
      try {
        textPane.setPage(getClass().getResource("PlatformIntro.html"));
      } catch (Exception e) {
        adminClientContext.log(e);
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

  @Override
  public Icon getIcon() {
    return DSOHelper.getHelper().getPlatformIcon();
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      diagnosticsPanel = null;
    }
  }
}
