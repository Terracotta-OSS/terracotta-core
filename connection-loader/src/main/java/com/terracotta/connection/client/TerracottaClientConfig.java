/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection.client;

import java.util.Set;


public class TerracottaClientConfig {
  private final String      tcConfigUrl;
  private final Set<String> tunnelledMBeanDomains;
  private final boolean     rejoin;
  private final boolean     nonStop;
  private final String      productId;
  private final ClassLoader classLoader;

  TerracottaClientConfig(TerracottaClientConfigParams params) {
    this.tcConfigUrl = params.getTcConfigUrl();
    this.tunnelledMBeanDomains = params.getTunnelledMBeanDomains();
    this.rejoin = params.isRejoin();
    this.nonStop = params.isNonStop();
    this.productId = params.getProductId();
    this.classLoader = params.getClassLoader();
  }

  public String getTcConfigUrl() {
    return this.tcConfigUrl;
  }

  public Set<String> getTunnelledMBeanDomains() {
    return tunnelledMBeanDomains;
  }

  public boolean isRejoin() {
    return rejoin;
  }

  public boolean isNonStopEnabled() {
    return nonStop;
  }

  public String getProductId() {
    return productId;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }
}
