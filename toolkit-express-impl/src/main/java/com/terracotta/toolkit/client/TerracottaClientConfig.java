/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import java.util.Set;

public class TerracottaClientConfig {

  private final String      tcConfigSnippetOrUrl;
  private final boolean     isUrl;
  private final Set<String> tunnelledMBeanDomains;
  private final boolean     rejoin;
  private final boolean     nonStop;

  TerracottaClientConfig(TerracottaClientConfigParams params) {
    this.tcConfigSnippetOrUrl = params.getTcConfigSnippetOrUrl();
    this.isUrl = params.isUrl();
    this.tunnelledMBeanDomains = params.getTunnelledMBeanDomains();
    this.rejoin = params.isRejoin();
    this.nonStop = params.isNonStop();
  }

  public String getTcConfigSnippetOrUrl() {
    return tcConfigSnippetOrUrl;
  }

  public boolean isUrl() {
    return isUrl;
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

}
