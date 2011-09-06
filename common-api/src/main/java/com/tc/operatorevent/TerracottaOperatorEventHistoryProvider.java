/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.List;

public interface TerracottaOperatorEventHistoryProvider {
  void push(TerracottaOperatorEvent event);

  List<TerracottaOperatorEvent> getOperatorEvents();
}
