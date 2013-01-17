/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.feature.SerializationFeature;
import org.terracotta.toolkit.serialization.ToolkitSerializer;

import com.terracotta.toolkit.feature.EnabledToolkitFeature;

public class SerializationFeatureImpl extends EnabledToolkitFeature implements SerializationFeature {

  private final ToolkitSerializer serializer;

  public SerializationFeatureImpl(SerializationStrategy strategy) {
    this.serializer = new ToolkitSerializerImpl(strategy);
  }

  @Override
  public ToolkitSerializer getDefaultToolkitSerializer() {
    return serializer;
  }

}
