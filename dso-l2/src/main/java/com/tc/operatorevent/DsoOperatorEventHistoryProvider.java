/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.CircularLossyQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DsoOperatorEventHistoryProvider implements TerracottaOperatorEventHistoryProvider {
  private final CircularLossyQueue<TerracottaOperatorEvent> operatorEventHistory;
  private final HashMap<String, Integer>                    unreadCounts;

  public DsoOperatorEventHistoryProvider() {
    operatorEventHistory = new CircularLossyQueue<TerracottaOperatorEvent>(TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OPERATOR_EVENTS_STORE, 1500));

    unreadCounts = new HashMap<String, Integer>();
    for (EventLevel severity : EventLevel.values()) {
      unreadCounts.put(severity.name(), Integer.valueOf(0));
    }
  }

  @Override
  public synchronized void push(TerracottaOperatorEvent event) {
    updateUnreadCounts(event, operatorEventHistory.push(event));
  }

  @Override
  public List<TerracottaOperatorEvent> getOperatorEvents() {
    TerracottaOperatorEvent[] operatorEvents = new TerracottaOperatorEventImpl[this.operatorEventHistory.depth()];
    this.operatorEventHistory.toArray(operatorEvents);
    return Arrays.asList(operatorEvents);
  }

  @Override
  public List<TerracottaOperatorEvent> getOperatorEvents(long sinceTimestamp) {
    Date dateSince = new Date(sinceTimestamp);
    List<TerracottaOperatorEvent> eventList = new ArrayList<TerracottaOperatorEvent>();
    TerracottaOperatorEvent[] operatorEvents = new TerracottaOperatorEventImpl[this.operatorEventHistory.depth()];
    this.operatorEventHistory.toArray(operatorEvents);
    for(TerracottaOperatorEvent e : operatorEvents ) {
      if((e != null) && e.getEventTime().after(dateSince)) {
        eventList.add(e);
      }
    }
    return eventList;
  }

  private void updateUnreadCounts(TerracottaOperatorEvent newEvent, TerracottaOperatorEvent removedEvent) {
    if (removedEvent != null && !removedEvent.isRead()) {
      incrementUnread(removedEvent, -1);
    }
    incrementUnread(newEvent, 1);
  }

  private void incrementUnread(TerracottaOperatorEvent operatorEvent, int count) {
    String eventLevelName = operatorEvent.getEventLevel().name();
    Integer value = unreadCounts.get(eventLevelName);
    unreadCounts.put(eventLevelName, Integer.valueOf(value.intValue() + count));
  }

  @Override
  public Map<String, Integer> getUnreadCounts() {
    return (Map<String, Integer>) unreadCounts.clone();
  }

  private boolean markRead(TerracottaOperatorEvent operatorEvent, boolean read) {
    if (read) {
      if (!operatorEvent.isRead()) {
        operatorEvent.markRead();
        incrementUnread(operatorEvent, -1);
        return true;
      }
      return false;
    } else {
      if (operatorEvent.isRead()) {
        operatorEvent.markUnread();
        incrementUnread(operatorEvent, 1);
        return true;
      }
      return false;
    }
  }

  @Override
  public synchronized boolean markOperatorEvent(TerracottaOperatorEvent operatorEvent, boolean read) {
    List<TerracottaOperatorEvent> operatorEvents = getOperatorEvents();
    for (TerracottaOperatorEvent event : operatorEvents) {
      if (event.equals(operatorEvent)) { return markRead(event, read); }
    }
    return false;
  }
}
