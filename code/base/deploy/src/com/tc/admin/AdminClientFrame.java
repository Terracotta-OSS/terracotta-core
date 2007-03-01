/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XFrame;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XTreeNode;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

public class AdminClientFrame extends XFrame
  implements AdminClientController
{
  private AdminClientPanel m_mainPanel;

  public AdminClientFrame() {
    super();

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(m_mainPanel = new AdminClientPanel());
    
    XMenuBar menuBar;
    m_mainPanel.initMenubar(menuBar = new XMenuBar());
    setMenubar(menuBar);

    setTitle(AdminClient.getContext().getMessage("title"));
    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  public boolean isExpanded(XTreeNode node) {
    return m_mainPanel.isExpanded(node);
  }

  public void expand(XTreeNode node) {
    m_mainPanel.expand(node);
  }

  public boolean isSelected(XTreeNode node) {
    return m_mainPanel.isSelected(node);
  }

  public void select(XTreeNode node) {
    m_mainPanel.select(node);
  }

  public void remove(XTreeNode node) {
    m_mainPanel.remove(node);
  }

  public void nodeStructureChanged(XTreeNode node) {
    m_mainPanel.nodeStructureChanged(node);
  }

  public void nodeChanged(XTreeNode node) {
    m_mainPanel.nodeChanged(node);
  }

  private Preferences getPreferences() {
    AdminClientContext acc = AdminClient.getContext();
    return acc.prefs.node("AdminClientFrame");
  }

  private void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

 public void updateServerPrefs() {
    m_mainPanel.updateServerPrefs();
  }

  protected void processWindowEvent(final WindowEvent e) {
    if(e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
    super.processWindowEvent(e);
  }

  public void log(String s) {
    m_mainPanel.log(s);
  }

  public void log(Exception e) {
    m_mainPanel.log(e);
  }

  public void setStatus(String msg) {
    m_mainPanel.setStatus(msg);
  }

  public void clearStatus() {
    m_mainPanel.clearStatus();
  }

  public Rectangle getDefaultBounds() {
    Toolkit               tk     = Toolkit.getDefaultToolkit();
    Dimension             size   = tk.getScreenSize();
    GraphicsEnvironment   env    = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice        device = env.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
    Insets                insets = tk.getScreenInsets(config);

    size.width  -= (insets.left+insets.right);
    size.height -= (insets.top+insets.bottom);

    int width  = (int)(size.width * 0.75f);
    int height = (int)(size.height * 0.66f);

    // center
    int x = size.width/2  - width/2;
    int y = size.height/2 - height/2;

    return new Rectangle(x, y, width, height);
  }

  private String getBoundsString() {
    Rectangle b = getBounds();
    return b.x+","+b.y+","+b.width+","+b.height;
  }

  private int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    }
    catch(Exception e) {
      return 0;
    }
  }

  private Rectangle parseBoundsString(String s) {
    String[] split  = s.split(",");
    int      x      = parseInt(split[0]);
    int      y      = parseInt(split[1]);
    int      width  = parseInt(split[2]);
    int      height = parseInt(split[3]);

    return new Rectangle(x,y,width,height);
  }

  public void storeBounds() {
    String name = getName();

    if(name == null) {
      return;
    }

    int state = getExtendedState();
    if((state & NORMAL) != NORMAL) {
      return;
    }
    
    Preferences prefs = getPreferences();

    prefs.put("Bounds", getBoundsString());
    storePreferences();
  }

  protected Rectangle getPreferredBounds() {
    Preferences prefs = getPreferences();
    String      s     = prefs.get("Bounds", null);

    return s != null ? parseBoundsString(s) : getDefaultBounds();
  }

  public void addServerLog(ConnectionContext cc) {
    m_mainPanel.addServerLog(cc);
  }

  public void removeServerLog(ConnectionContext cc) {
    m_mainPanel.removeServerLog(cc);
  }
}
