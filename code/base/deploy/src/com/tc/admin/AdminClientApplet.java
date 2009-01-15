/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XApplet;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XTreeNode;

import java.awt.BorderLayout;

public class AdminClientApplet extends XApplet implements AdminClientController {
  private AdminClientPanel mainPanel;

  public AdminClientApplet(IAdminClientContext adminClientContext) {
    super();

    getContentPane().setLayout(new BorderLayout());
    mainPanel = new AdminClientPanel(adminClientContext);
    getContentPane().add(mainPanel, BorderLayout.CENTER);

    XMenuBar menuBar;
    mainPanel.initMenubar(menuBar = new XMenuBar());
    setJMenuBar(menuBar);
  }

  public boolean selectNode(XTreeNode startNode, String name) {
    return mainPanel.selectNode(startNode, name);
  }

  public boolean isExpanded(XTreeNode node) {
    return mainPanel.isExpanded(node);
  }

  public void expand(XTreeNode node) {
    mainPanel.expand(node);
  }

  public void expandAll(XTreeNode node) {
    mainPanel.expandAll(node);
  }
  
  public boolean isSelected(XTreeNode node) {
    return mainPanel.isSelected(node);
  }

  public void select(XTreeNode node) {
    mainPanel.select(node);
  }

  public boolean testServerMatch(ClusterNode node) {
    return mainPanel.testServerMatch(node);
  }

  public void updateServerPrefs() {
    mainPanel.updateServerPrefs();
  }

  public void stop() {
    mainPanel.disconnectAll();
  }

  public void log(String s) {
    mainPanel.log(s);
  }

  public void log(Throwable t) {
    mainPanel.log(t);
  }

  public void setStatus(String msg) {
    mainPanel.setStatus(msg);
  }

  public void clearStatus() {
    mainPanel.clearStatus();
  }

  public void block() {/**/
  }

  public void unblock() {/**/
  }

  public void showOption(String optionName) {
    mainPanel.showOption(optionName);
  }

  public void showOptions() {
    mainPanel.showOptions();
  }
}
