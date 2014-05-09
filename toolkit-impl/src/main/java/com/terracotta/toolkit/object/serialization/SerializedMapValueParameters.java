/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.store.ToolkitConfigFields;


public class SerializedMapValueParameters<T> {

  private volatile T      deserialized;
  private volatile byte[] serialized;
  private volatile int    createTime;
  private volatile int    lastAccessedTime;
  private volatile int    customTTI;
  private volatile int    customTTL;

  public T getDeserialized() {
    return deserialized;
  }

  public boolean isCustomLifespan() {
    return customTTI != ToolkitConfigFields.NO_MAX_TTI_SECONDS || customTTL != ToolkitConfigFields.NO_MAX_TTL_SECONDS;
  }

  public int getCustomTTI() {
    return customTTI;
  }

  public SerializedMapValueParameters setCustomTTI(int customTTI) {
    this.customTTI = customTTI;
    return this;
  }

  public int getCustomTTL() {
    return customTTL;
  }

  public SerializedMapValueParameters setCustomTTL(int customTTL) {
    this.customTTL = customTTL;
    return this;
  }

  public void setDeserialized(T deserialized) {
    this.deserialized = deserialized;
  }

  public byte[] getSerialized() {
    return serialized;
  }

  public void setSerialized(byte[] serialized) {
    this.serialized = serialized;
  }

  public int getCreateTime() {
    return createTime;
  }

  public void setCreateTime(int createTime) {
    this.createTime = createTime;
  }

  public int getLastAccessedTime() {
    return lastAccessedTime;
  }

  public void setLastAccessedTime(int lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
  }

  public SerializedMapValueParameters<T> deserialized(T deserializedParam) {
    this.setDeserialized(deserializedParam);
    return this;
  }

  public SerializedMapValueParameters<T> serialized(byte[] serializedParam) {
    this.setSerialized(serializedParam);
    return this;
  }

  public SerializedMapValueParameters<T> createTime(int createTimeParam) {
    this.setCreateTime(createTimeParam);
    return this;
  }

  public SerializedMapValueParameters<T> lastAccessedTime(int lastAccessedTimeParam) {
    this.setLastAccessedTime(lastAccessedTimeParam);
    return this;
  }

}
