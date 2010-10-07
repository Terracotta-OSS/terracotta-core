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
  private final String         collapseString;

  public TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subSystem, String message,
                                     String collapseString) {
    this.eventType = eventType;
    this.subSystem = subSystem;
    this.time = System.currentTimeMillis();
    this.eventMessage = message;
    this.collapseString = collapseString;
  }

  private TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subsystem, long time, String nodeName,
                                      String message, String collapseString) {
    this.eventType = eventType;
    this.subSystem = subsystem;
    this.time = time;
    this.nodeName = nodeName;
    this.eventMessage = message;
    this.collapseString = collapseString;
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

  public String getCollapseString() {
    return collapseString;
  }

  public int compareTo(TerracottaOperatorEventImpl o) {
    return (int) (this.time - o.time);
  }

  public boolean isRead() {
    return this.isRead;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TerracottaOperatorEventImpl)) return false;
    TerracottaOperatorEventImpl event = (TerracottaOperatorEventImpl) o;
    if (this.eventType != event.eventType) return false;
    if (this.subSystem != event.subSystem) return false;
    if (!this.collapseString.equals(event.collapseString)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((collapseString == null) ? 0 : collapseString.hashCode());
    result = prime * result + ((eventMessage == null) ? 0 : eventMessage.hashCode());
    result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
    result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
    result = prime * result + ((subSystem == null) ? 0 : subSystem.hashCode());
    result = prime * result + (int) (time ^ (time >>> 32));
    return result;
  }

  public void markRead() {
    this.isRead = true;
  }

  public void markUnread() {
    this.isRead = false;
  }

  @Override
  public String toString() {
    return getEventType() + " " + getEventTime() + " " + getNodeName() + " " + getEventSubsystemAsString() + " "
           + getEventMessage();
  }

  public String extractAsText() {
    return toString();
  }

  @Override
  public TerracottaOperatorEvent clone() {
    return new TerracottaOperatorEventImpl(this.eventType, this.subSystem, this.time, this.nodeName, this.eventMessage,
                                           this.collapseString);
  }

}
