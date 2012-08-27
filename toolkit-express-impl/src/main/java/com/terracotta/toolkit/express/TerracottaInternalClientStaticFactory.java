/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.client.TerracottaClientConfig;

public class TerracottaInternalClientStaticFactory {

  private static final TerracottaInternalClientFactory INSTANCE;

  static {
    INSTANCE = new TerracottaInternalClientFactoryImpl();
  }

  private TerracottaInternalClientStaticFactory() {
    // not instantiable
  }

  public static TerracottaInternalClient getOrCreateTerracottaInternalClient(TerracottaClientConfig config) {
    return INSTANCE.getOrCreateL1Client(config);
  }
}
