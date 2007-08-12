/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.awt.BorderLayout;

import com.tc.admin.common.XApplet;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XTreeNode;

public class AdminClientApplet extends XApplet
  implements AdminClientController
{
  private AdminClientPanel m_mainPanel;

  public AdminClientApplet() {
    super();

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(m_mainPanel = new AdminClientPanel());
    
    XMenuBar menuBar;
    m_mainPanel.initMenubar(menuBar = new XMenuBar());
    setMenubar(menuBar);
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

  public boolean testServerMatch(ServerNode node) {
    return m_mainPanel.testServerMatch(node);
  }
  
 public void updateServerPrefs() {
    m_mainPanel.updateServerPrefs();
  }

  public void stop() {
    m_mainPanel.disconnectAll();
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

  public void addServerLog(ConnectionContext cc) {
    m_mainPanel.addServerLog(cc);
  }

  public void removeServerLog(ConnectionContext cc) {
    m_mainPanel.removeServerLog(cc);
  }
  
  public void block() {/**/}
  public void unblock() {/**/}
}
