/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.terracotta.connection.client.TerracottaClientConfig;

/**
 * A factory for creating {@link TerracottaInternalClient}
 */
public interface TerracottaInternalClientFactory {

  /**
   * Get or create a new client depending on config
   */
  public TerracottaInternalClient createL1Client(TerracottaClientConfig config);

}
