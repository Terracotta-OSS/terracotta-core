/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.CircularLossyQueue;

import java.util.Arrays;
import java.util.List;

public class DsoOperatorEventHistoryProvider implements TerracottaOperatorEventHistoryProvider {
  private final CircularLossyQueue<TerracottaOperatorEvent> operatorEventHistory = new CircularLossyQueue<TerracottaOperatorEvent>(
                                                                                                                                   TCPropertiesImpl
                                                                                                                                       .getProperties()
                                                                                                                                       .getInt(
                                                                                                                                               TCPropertiesConsts.L2_OPERATOR_EVENTS_STORE,
                                                                                                                                               1500));

  public void push(TerracottaOperatorEvent event) {
    operatorEventHistory.push(event);
  }

  public List<TerracottaOperatorEvent> getOperatorEvents() {
    TerracottaOperatorEvent[] operatorEvents = new TerracottaOperatorEventImpl[this.operatorEventHistory.depth()];
    this.operatorEventHistory.toArray(operatorEvents);
    return Arrays.asList(operatorEvents);
  }

}
