/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.client;

public final class ClientMode {

  public static final ClientMode EXPRESS_REJOIN_MODE = new ClientMode("Express Rejoin Client");
  public static final ClientMode DSO_MODE            = new ClientMode("DSO Client");

  private final String           name;

  private ClientMode(final String clientDescription) {
    this.name = clientDescription;
  }

  public String getName() {
    return name;
  }

  public boolean isExpressRejoinClient() {
    return this == EXPRESS_REJOIN_MODE;
  }

  public boolean isDSOClient() {
    return this == DSO_MODE;
  }
}
