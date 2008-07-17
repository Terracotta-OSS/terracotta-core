/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRAL1ReferenceCount implements StatisticRetrievalAction {

  public SRAL1ReferenceCount(ClientStateManager clientStateManager) {
    // dummy
  }

  public String getName() {
    return "i suck";
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] { new StatisticData("i suck", 1L) };
  }

}
