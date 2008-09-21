/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.net.NodeID;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 */
public class SRAL1ReferenceCount implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "l1 reference count";

  private final ClientStateManager clientStateManager;

  public SRAL1ReferenceCount(final ClientStateManager clientStateManager) {
    Assert.assertNotNull("clientStateManager", clientStateManager);
    this.clientStateManager = clientStateManager;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    List<StatisticData> data = new ArrayList<StatisticData>();
    synchronized (clientStateManager) {
      Set<NodeID> nodeIDs = clientStateManager.getConnectedClientIDs();
      for (final NodeID nodeID : nodeIDs) {
        data.add(new StatisticData(ACTION_NAME, nodeID.toString(), (long)clientStateManager.getReferenceCount(nodeID)));
      }
    }
    StatisticData[] result = new StatisticData[data.size()];
    return data.toArray(result);
  }
}