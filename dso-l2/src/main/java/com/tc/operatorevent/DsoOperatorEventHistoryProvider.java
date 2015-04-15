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
