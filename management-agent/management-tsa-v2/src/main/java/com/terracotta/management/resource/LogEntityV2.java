/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a server log
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class LogEntityV2 extends AbstractTsaEntityV2 {

  private String sourceId;
  private long timestamp;
  private String message;
  private String[] throwableStringRep;

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

  public void setThrowableStringRep(String[] throwableStringRep) {
    this.throwableStringRep = throwableStringRep;
  }

  public String[] getThrowableStringRep() {
    return this.throwableStringRep;
  }
}
