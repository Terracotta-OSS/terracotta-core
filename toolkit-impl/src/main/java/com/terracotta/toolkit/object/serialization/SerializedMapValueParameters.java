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
