/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

public class ClusteredStringBuilderFactoryImpl implements ClusteredStringBuilderFactory {
  private static final String                PREFIX                         = "__toolkit@";
  public static final String                 CLUSTERED_TEXT_BUCKET_MAP_NAME = PREFIX + "clusteredStringBuilderMap";

  private final ToolkitStore<String, String> buckets;

  public ClusteredStringBuilderFactoryImpl(Toolkit toolkit) {
    ToolkitStoreConfigBuilder builder = new ToolkitStoreConfigBuilder();
    Configuration configuration = builder.consistency(Consistency.STRONG).build();
    this.buckets = toolkit.getStore(CLUSTERED_TEXT_BUCKET_MAP_NAME, configuration, null);
  }

  @Override
  public ClusteredStringBuilder getClusteredStringBuilder(String name) {
    return new ClusteredStringBuilderImpl(name, buckets);
  }

}
