/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.serialization.Serialization;
import org.terracotta.toolkit.serialization.ToolkitSerializer;

public class SerializationFeatureImpl implements Serialization {

  private final ToolkitSerializer     serializer;

  public SerializationFeatureImpl(SerializationStrategy strategy) {
    this.serializer = new ToolkitSerializerImpl(strategy);

  }

  @Override
  public ToolkitSerializer getDefaultToolkitSerializer() {
    return serializer;
  }

}
