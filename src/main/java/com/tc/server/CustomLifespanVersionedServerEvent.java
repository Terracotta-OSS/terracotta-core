/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

public class CustomLifespanVersionedServerEvent implements VersionedServerEvent {

  private final VersionedServerEvent basicServerEvent;
  private final int creationTimeInSeconds;
  private final int timeToIdle;
  private final int timeToLive;

  public CustomLifespanVersionedServerEvent(VersionedServerEvent basicServerEvent, int creationTimeInSeconds, int timeToIdle, int timeToLive) {
    this.basicServerEvent = basicServerEvent;
    this.creationTimeInSeconds = creationTimeInSeconds;
    this.timeToIdle = timeToIdle;
    this.timeToLive = timeToLive;
  }

  @Override
  public String getCacheName() {
    return basicServerEvent.getCacheName();
  }

  @Override
  public ServerEventType getType() {
    return basicServerEvent.getType();
  }

  @Override
  public void setType(ServerEventType type) {
    basicServerEvent.setType(type);
  }

  @Override
  public Object getKey() {
    return basicServerEvent.getKey();
  }

  @Override
  public byte[] getValue() {
    return basicServerEvent.getValue();
  }

  @Override
  public void setValue(byte[] value) {
    basicServerEvent.setValue(value);
  }

  @Override
  public long getVersion() {
    return basicServerEvent.getVersion();
  }


  public int getTimeToIdle() {
    return timeToIdle;
  }

  public int getTimeToLive() {
    return timeToLive;
  }

  public int getCreationTimeInSeconds() {
    return creationTimeInSeconds;
  }

  @Override
  public String toString() {
    return getClass().getName() + "(" + "basicServerEvent=" + basicServerEvent + ",creationTimeInSeconds="
           + creationTimeInSeconds + ",timeToIdle" + timeToIdle + ",timeToLive=" + timeToLive + ")";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final CustomLifespanVersionedServerEvent that = (CustomLifespanVersionedServerEvent) o;

    if (creationTimeInSeconds != that.creationTimeInSeconds) return false;
    if (timeToIdle != that.timeToIdle) return false;
    if (timeToLive != that.timeToLive) return false;
    if (!basicServerEvent.equals(that.basicServerEvent)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = basicServerEvent.hashCode();
    result = 31 * result + creationTimeInSeconds;
    result = 31 * result + timeToIdle;
    result = 31 * result + timeToLive;
    return result;
  }
}
