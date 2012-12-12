/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing an operator event
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class OperatorEventEntity extends AbstractTsaEntity {

  private String sourceId;
  private long timestamp;
  private String message;
  private String collapseString;
  private String eventSubsystem;
  private String eventType;
  private boolean read;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCollapseString() {
    return collapseString;
  }

  public void setCollapseString(String collapseString) {
    this.collapseString = collapseString;
  }

  public String getEventSubsystem() {
    return eventSubsystem;
  }

  public void setEventSubsystem(String eventSubsystem) {
    this.eventSubsystem = eventSubsystem;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }
}
