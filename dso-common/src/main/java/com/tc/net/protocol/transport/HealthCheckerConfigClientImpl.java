/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

public class HealthCheckerConfigClientImpl extends HealthCheckerConfigImpl {
  private String callbackportListenerBindAddress;
  private int    callbackportListenerBindPort = TransportHandshakeMessage.NO_CALLBACK_PORT;

  public HealthCheckerConfigClientImpl(TCProperties healthCheckerProperties, String hcName) {
    super(healthCheckerProperties, hcName);
    this.callbackportListenerBindAddress = healthCheckerProperties.getProperty("bindAddress");
    this.callbackportListenerBindPort = healthCheckerProperties.getInt("bindPort", 0);
  }

  public HealthCheckerConfigClientImpl(String name) {
    super(name);
    this.callbackportListenerBindPort = 0;
  }

  public HealthCheckerConfigClientImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                       int socketConnectMaxCount, int socketConnectTimeout, String bindAddress,
                                       int bindPort) {
    super(idle, interval, probes, name, extraCheck, socketConnectMaxCount, socketConnectTimeout);
    this.callbackportListenerBindAddress = bindAddress;
    this.callbackportListenerBindPort = bindPort;
  }

  public boolean isCallbackPortListenerNeeded() {
    return true;
  }

  @Override
  public String getCallbackPortListenerBindAddress() {
    return this.callbackportListenerBindAddress;
  }

  @Override
  public int getCallbackPortListenerBindPort() {
    return this.callbackportListenerBindPort;
  }

}
