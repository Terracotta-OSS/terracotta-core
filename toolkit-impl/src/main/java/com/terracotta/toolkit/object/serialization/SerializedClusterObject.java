/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;


public interface SerializedClusterObject<T> {
  byte[] getBytes();

  T getValue(SerializationStrategy strategy, boolean compression, boolean local);
}
