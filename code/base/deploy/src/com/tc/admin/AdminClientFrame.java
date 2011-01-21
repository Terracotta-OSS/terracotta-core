/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XFrame;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XTreeNode;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.util.prefs.Preferences;

public class AdminClientFrame extends XFrame implements AdminClientController {
  private final IAdminClientContext adminClientContext;
  private final AdminClientPanel    mainPanel;

  public AdminClientFrame(IAdminClientContext adminClientContext) {
    super();

    this.adminClientContext = adminClientContext;

    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/tc/admin/icons/logo_small.png")));

    getContentPane().setLayout(new BorderLayout());
    mainPanel = createAdminClientPanel(adminClientContext);
    getContentPane().add(mainPanel, BorderLayout.CENTER);

    XMenuBar menuBar;
    mainPanel.initMenubar(menuBar = new XMenuBar());
    setJMenuBar(menuBar);

    setTitle(adminClientContext.getMessage("title"));
    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  protected AdminClientPanel createAdminClientPanel(IAdminClientContext context) {
    return new AdminClientPanel(context);
  }

  @Override
  protected boolean shouldClose() {
    // may terminate after safety checks otherwise don't close
    mainPanel.handleQuit();
    return false;
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

  @Override
  protected Preferences getPreferences() {
    return adminClientContext.getPrefs().node("AdminClientFrame");
  }

  @Override
  protected void storePreferences() {
    adminClientContext.storePrefs();
  }

  public void updateServerPrefs() {
    mainPanel.updateServerPrefs();
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

  public void showOption(String optionName) {
    mainPanel.showOption(optionName);
  }

  public void showOptions() {
    mainPanel.showOptions();
  }
}
