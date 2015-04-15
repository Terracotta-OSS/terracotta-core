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

import com.tc.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent, Comparable<TerracottaOperatorEventImpl> {
  private final long                    time;
  private final String                  eventMessage;
  private final EventLevel              eventLevel;
  private final EventType               eventType;
  private final EventSubsystem          subSystem;
  private volatile Map<String, Integer> nodes;
  private boolean                       isRead = false;
  private final String                  collapseString;

  public TerracottaOperatorEventImpl(EventLevel eventLevel, EventSubsystem subSystem, EventType eventType,
                                     String message,
                                     String collapseString) {
    this(eventLevel, subSystem, eventType, System.currentTimeMillis(), message, collapseString,
         new HashMap<String, Integer>());
  }

  public TerracottaOperatorEventImpl(EventLevel eventLevel, EventSubsystem subSystem, EventType eventType,
                                     String message,
                                     long time, String collapseString) {
    this(eventLevel, subSystem, eventType, time, message, collapseString, new HashMap<String, Integer>());
  }

  private TerracottaOperatorEventImpl(EventLevel eventLevel, EventSubsystem subsystem, EventType eventType, long time,
                                      String message,
                                      String collapseString, Map<String, Integer> nodes) {
    // Using a CHM here can trigger CDV-1377 if event sent/serialized from a custom mode L1
    if (nodes instanceof ConcurrentHashMap) { throw new AssertionError("CHM not allowed here"); }

    this.eventLevel = eventLevel;
    this.subSystem = subsystem;
    this.time = time;
    this.eventMessage = message;
    this.collapseString = collapseString;
    this.nodes = nodes;
    this.eventType = eventType;
  }

  @Override
  public String getEventMessage() {
    return this.eventMessage;
  }

  @Override
  public Date getEventTime() {
    return new Date(this.time);
  }

  @Override
  public EventLevel getEventLevel() {
    return this.eventLevel;
  }

  @Override
  public String getEventLevelAsString() {
    return this.eventLevel.name();
  }

  @Override
  public EventType getEventType() {
    return this.eventType;
  }

  @Override
  public String getEventTypeAsString() {
    return this.eventType.name();
  }

  @Override
  public synchronized String getNodeName() {
    StringBuilder val = new StringBuilder();
    for (Entry<String, Integer> node : this.nodes.entrySet()) {
      Assert.assertTrue(node.getValue().intValue() >= 1);
      val.append(node.getKey());
      if (node.getValue().intValue() > 1) {
        val.append("(").append(node.getValue().intValue()).append(")");
      }
      val.append(" ");
    }
    return val.toString().trim();
  }

  @Override
  public synchronized void addNodeName(String nodeId) {
    Integer numOfSuchEvents = this.nodes.get(nodeId);
    if (numOfSuchEvents == null) {
      this.nodes.put(nodeId, 1);
    } else {
      this.nodes.put(nodeId, numOfSuchEvents.intValue() + 1);
    }
  }

  @Override
  public EventSubsystem getEventSubsystem() {
    return this.subSystem;
  }

  @Override
  public String getEventSubsystemAsString() {
    return this.subSystem.name();
  }

  @Override
  public String getCollapseString() {
    return collapseString;
  }

  @Override
  public int compareTo(TerracottaOperatorEventImpl o) {
    return (int) (this.time - o.time);
  }

  @Override
  public boolean isRead() {
    return this.isRead;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TerracottaOperatorEventImpl)) return false;
    TerracottaOperatorEventImpl event = (TerracottaOperatorEventImpl) o;
    if (this.eventLevel != event.eventLevel) return false;
    if (this.subSystem != event.subSystem) return false;
    if (this.eventType != event.eventType) return false;
    if (this.time != event.time) return false;
    if (!this.collapseString.equals(event.collapseString)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((collapseString == null) ? 0 : collapseString.hashCode());
    result = prime * result + ((eventLevel == null) ? 0 : eventLevel.hashCode());
    result = prime * result + ((subSystem == null) ? 0 : subSystem.hashCode());
    result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
    result = prime * result + (int) (time ^ (time >>> 32));
    return result;
  }

  @Override
  public void markRead() {
    this.isRead = true;
  }

  @Override
  public void markUnread() {
    this.isRead = false;
  }

  @Override
  public String toString() {
    return getEventLevel() + " " + getEventTime() + " " + getNodeName() + " " + getEventSubsystemAsString() + " "
           + getEventTypeAsString() + " "
           + getEventMessage();
  }

  @Override
  public String extractAsText() {
    return toString();
  }

  @Override
  public TerracottaOperatorEvent cloneEvent() {
    Map<String, Integer> nodesCopy;
    synchronized (this) {
      nodesCopy = new HashMap<String, Integer>(this.nodes);
    }
    return new TerracottaOperatorEventImpl(this.eventLevel, this.subSystem, this.eventType, this.time,
                                           this.eventMessage,
                                           this.collapseString, nodesCopy);

  }

  // STRICTLY FOR TESTS
  public Map<String, Integer> getNodes() {
    return this.nodes;
  }
}
