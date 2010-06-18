/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.Date;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent, Comparable<TerracottaOperatorEventImpl> {
  private final long           time;
  private final String         eventMessage;
  private final EventType      eventType;
  private final EventSubsystem subSystem;
  private String               nodeName = null;
  private boolean              isRead   = false;

  public TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subSystem, String message) {
    this.eventType = eventType;
    this.subSystem = subSystem;
    this.time = System.currentTimeMillis();
    this.eventMessage = message;
  }

  public String getEventMessage() {
    return this.eventMessage;
  }

  public Date getEventTime() {
    return new Date(this.time);
  }

  public EventType getEventType() {
    return this.eventType;
  }

  public String getEventTypeAsString() {
    return this.eventType.name();
  }

  public String getNodeName() {
    return this.nodeName;
  }

  public void setNodeName(String nodeId) {
    this.nodeName = nodeId;
  }

  public EventSubsystem getEventSubsystem() {
    return this.subSystem;
  }

  public String getEventSubsystemAsString() {
    return this.subSystem.name();
  }

  public int compareTo(TerracottaOperatorEventImpl o) {
    return (int) (this.time - o.time);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TerracottaOperatorEventImpl)) return false;
    TerracottaOperatorEventImpl event = (TerracottaOperatorEventImpl) o;
    if (this.eventType != event.eventType) return false;
    if (this.subSystem != event.subSystem) return false;
    if (!this.eventMessage.equals(event.eventMessage)) return false;
    return true;
  }

  public boolean isRead() {
    return this.isRead;
  }

  public void markRead() {
    this.isRead = true;
  }
  
  @Override
  public String toString() {
    return getEventType() + " " + getEventTime() + " " + getNodeName() + " " + getEventSubsystemAsString() + " "
           + getEventMessage();
  }

  public String extractAsText() {
    return toString();
  }
  
}
