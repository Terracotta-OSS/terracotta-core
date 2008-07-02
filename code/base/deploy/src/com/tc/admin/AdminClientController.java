/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IServer;

public interface AdminClientController {
  void    setStatus(String msg);
  void    clearStatus();
  void    expand(XTreeNode node);
  boolean isExpanded(XTreeNode node);
  void    select(XTreeNode node);
  boolean isSelected(XTreeNode node);
  void    nodeStructureChanged(XTreeNode node);
  void    remove(XTreeNode node);
  void    nodeChanged(XTreeNode node);
  boolean testServerMatch(ServerNode node);
  boolean testServerMatch(ClusterNode node);
  void    log(String msg);
  void    log(Throwable t);
  void    block();
  void    unblock();
  void    updateServerPrefs();
  void    addServerLog(IServer server);
  void    removeServerLog(IServer server);
}
