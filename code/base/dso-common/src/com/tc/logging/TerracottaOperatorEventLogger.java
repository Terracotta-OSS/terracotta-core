/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.NodeNameProvider;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;

import java.util.concurrent.CopyOnWriteArrayList;

public class TerracottaOperatorEventLogger {

  private final CopyOnWriteArrayList<TerracottaOperatorEventCallback>        callbacks        = new CopyOnWriteArrayList<TerracottaOperatorEventCallback>();
  private final CopyOnWriteArrayList<TerracottaOperatorEventHistoryProvider> historyProviders = new CopyOnWriteArrayList<TerracottaOperatorEventHistoryProvider>();
  private final NodeNameProvider                                             nodeNameProvider;

  public TerracottaOperatorEventLogger(NodeNameProvider nodeIdProvider) {
    this.nodeNameProvider = nodeIdProvider;
  }

  public void registerEventCallback(TerracottaOperatorEventCallback callback) {
    this.callbacks.add(callback);
  }

  public void fireOperatorEvent(TerracottaOperatorEvent event) {
    event.addNodeName(this.nodeNameProvider.getNodeName());
    for (TerracottaOperatorEventHistoryProvider historyProvider : this.historyProviders) {
      historyProvider.push(event);
    }
    for (TerracottaOperatorEventCallback callback : this.callbacks) {
      callback.logOperatorEvent(event);
    }
  }

  public void registerForHistory(TerracottaOperatorEventHistoryProvider historyProvider) {
    this.historyProviders.add(historyProvider);
  }
}
