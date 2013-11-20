/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

public class CustomLifespanVersionedServerEvent implements VersionedServerEvent {

  private final BasicServerEvent basicServerEvent;
  private final int              creationTimeInSeconds;
  private final int              timeToIdle;
  private final int              timeToLive;

  public CustomLifespanVersionedServerEvent(BasicServerEvent basicServerEvent, int creationTimeInSeconds, int timeToIdle, int timeToLive) {
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
    return "AdvancedServerEvent [basicServerEvent=" + basicServerEvent + ", timeToIdle=" + timeToIdle + ", timeToLive="
           + timeToLive + ", creationTimeInSeconds=" + creationTimeInSeconds + "]";
  }

}
