/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.operatorevent;

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
