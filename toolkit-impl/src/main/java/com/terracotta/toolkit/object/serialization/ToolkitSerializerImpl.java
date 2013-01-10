/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.serialization.ToolkitSerializer;

public class ToolkitSerializerImpl implements ToolkitSerializer {

  private final SerializationStrategy serializationStrategy;

  public ToolkitSerializerImpl(SerializationStrategy strategy) {
    this.serializationStrategy = strategy;
  }

  @Override
  public <T> String serializeToString(T object) {
    return serializationStrategy.serializeToString(object);
  }

  @Override
  public <T> T deserializeFromString(String serializedString) {
    return deserialize(serializedString, false);
  }

  @Override
  public <T> T deserializeFromStringLocally(String serializedString) {
    return deserialize(serializedString, true);
  }

  private <T> T deserialize(String serializedString, boolean localOnly) {
    try {
      return (T) serializationStrategy.deserializeFromString(serializedString, localOnly);
    } catch (Exception e) {
      throw new ToolkitRuntimeException(e);
    }
  }

}
