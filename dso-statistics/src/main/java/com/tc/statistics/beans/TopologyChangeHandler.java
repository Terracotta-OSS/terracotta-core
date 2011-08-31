/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.statistics.StatisticsManager;

import java.io.Serializable;

public interface TopologyChangeHandler extends Serializable {
  public void agentAdded(StatisticsManager agent);
}
