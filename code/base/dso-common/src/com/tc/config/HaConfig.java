/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;

public interface HaConfig {

  /**
   * Returns true if more than 1 ActiveServerGroup's are defined
   */
  boolean isActiveActive();
  
  /**
   * Returns true if not active-active and ha mode is set to networked-active-passive 
   */
  boolean isNetworkedActivePassive();

  /**
   * Returns true if not active-active and not networked-active-passive 
   */
  boolean isDiskedBasedActivePassive();

  ServerGroup getActiveCoordinatorGroup();
  
  ServerGroup[] getAllActiveServerGroups();

  Node[] makeAllNodes();

  Node makeThisNode();
  
  Node[] getAllNodes();
}
