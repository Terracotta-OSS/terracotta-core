/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTreeNode;

public interface AdminClientController extends ApplicationController {
  void expand(XTreeNode node);

  boolean isExpanded(XTreeNode node);

  void expandAll(XTreeNode node);

  boolean selectNode(XTreeNode startNode, String name);

  void select(XTreeNode node);

  boolean isSelected(XTreeNode node);

  boolean testServerMatch(ClusterNode node);

  void updateServerPrefs();

  void activeFeatureAdded(String name);

  void activeFeatureRemoved(String name);
}
