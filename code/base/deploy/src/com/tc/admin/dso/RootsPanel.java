/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Label;

import com.tc.admin.AdminClient;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterNode;
import com.tc.util.ProductInfo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

public class RootsPanel extends XContainer {
  protected IClusterNode        m_clusterNode;
  protected Label               m_liveObjectCountValueLabel;
  protected BasicObjectSetPanel m_objectSetPanel;
  protected MouseListener       m_objectSetMouseListener;

  public RootsPanel(IClusterNode clusterNode, IBasicObject[] roots) {
    super();

    load((ContainerResource) AdminClient.getContext().getComponent("RootsPanel"));

    m_clusterNode = clusterNode;
    String tip = AdminClient.getContext().getString("liveObjectCount.tip");
    ((Label) findComponent("LiveObjectCountLabel")).setToolTipText(tip);
    m_liveObjectCountValueLabel = (Label) findComponent("LiveObjectCountValueLabel");
    m_liveObjectCountValueLabel.setToolTipText(tip);
    ((Button) findComponent("HelpButton")).addActionListener(new LiveObjectHelpAction());
    m_objectSetPanel = (BasicObjectSetPanel) findComponent("ObjectSetPanel");

    m_objectSetPanel.setObjects(clusterNode, roots);
    m_objectSetMouseListener = new ObjectSetMouseListener();
    m_objectSetPanel.getTree().addMouseListener(m_objectSetMouseListener);
  }

  private class ObjectSetMouseListener extends MouseAdapter implements Serializable {
    public void mouseClicked(MouseEvent e) {
      updateLiveObjectCount();
    }
  }

  private void updateLiveObjectCount() {
    m_liveObjectCountValueLabel.setText(Integer.toString(m_clusterNode.getLiveObjectCount()));
  }

  public void setObjects(IBasicObject[] roots) {
    updateLiveObjectCount();
    m_objectSetPanel.setObjects(m_clusterNode, roots);
  }

  public void clearModel() {
    m_objectSetPanel.clearModel();
  }

  public void refresh() {
    updateLiveObjectCount();
    m_objectSetPanel.refresh();
  }

  public void add(IBasicObject root) {
    m_objectSetPanel.add(root);
  }
  
  private class LiveObjectHelpAction implements ActionListener {
    private String getKitID() {
      String kitID = ProductInfo.getInstance().kitID();
      if(ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
        kitID = System.getProperty("com.tc.kitID", "42.0");
      }
      return kitID;
    }
    
    public void actionPerformed(ActionEvent e) {
      String kitID =getKitID();
      String loc = "http://www.terracotta.org/kit/reflector?kitID=" + kitID
                   + "&pageID=ConsoleGuide#AdminConsoleGuide-Roots";
      BrowserLauncher.openURL(loc);
    }
  }

  public void tearDown() {
    m_objectSetPanel.getTree().removeMouseListener(m_objectSetMouseListener);

    super.tearDown();

    m_clusterNode = null;
    m_liveObjectCountValueLabel = null;
    m_objectSetPanel = null;
    m_objectSetMouseListener = null;
  }
}
