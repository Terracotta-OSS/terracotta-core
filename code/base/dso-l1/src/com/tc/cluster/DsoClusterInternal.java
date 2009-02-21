/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cluster;

public interface DsoClusterInternal extends DsoCluster {

  public void fireThisNodeJoined(String nodeId, String[] clusterMembers);

  public void fireThisNodeLeft();

  public void fireNodeJoined(String nodeId);

  public void fireNodeLeft(String nodeId);

  public void fireOperationsEnabled();

  public void fireOperationsDisabled();

}