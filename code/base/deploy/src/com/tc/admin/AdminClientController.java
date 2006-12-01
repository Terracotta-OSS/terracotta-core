/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTreeNode;

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
  void    log(String msg);
  void    log(Exception e);
  void    block();
  void    unblock();
  void    updateServerPrefs();
  void    addServerLog(ConnectionContext cc);
  void    removeServerLog(ConnectionContext cc);
}
