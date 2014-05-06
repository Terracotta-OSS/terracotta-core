/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.List;
import java.util.Map;

public interface TerracottaOperatorEventHistoryProvider {
  void push(TerracottaOperatorEvent event);

  List<TerracottaOperatorEvent> getOperatorEvents();

  List<TerracottaOperatorEvent> getOperatorEvents(long sinceTimestamp);

  /**
   * Returns the unread event counts broken out by event type name.
   * 
   * @see TerracottaOperatorEvent.EventLevel
   */
  Map<String, Integer> getUnreadCounts();

  boolean markOperatorEvent(TerracottaOperatorEvent operatorEvent, boolean read);
}
