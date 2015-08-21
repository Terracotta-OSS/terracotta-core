/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;


import com.terracotta.connection.client.TerracottaClientConfig;

public class TerracottaInternalClientStaticFactory {

  private static final TerracottaInternalClientFactory INSTANCE;

  static {
    INSTANCE = new TerracottaInternalClientFactoryImpl();
  }

  private TerracottaInternalClientStaticFactory() {
    // not instantiable
  }

  public static TerracottaInternalClient getOrCreateTerracottaInternalClient(TerracottaClientConfig config) {
    return INSTANCE.createL1Client(config);
  }
}
