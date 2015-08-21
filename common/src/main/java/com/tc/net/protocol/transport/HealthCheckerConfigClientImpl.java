/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

import java.util.Collections;
import java.util.Set;

public class HealthCheckerConfigClientImpl extends HealthCheckerConfigImpl {
  private final String       callbackportListenerBindAddress;
  private final Set<Integer> callbackportListenerBindPort;

  public HealthCheckerConfigClientImpl(TCProperties healthCheckerProperties, String hcName) {
    super(healthCheckerProperties, hcName);
    this.callbackportListenerBindAddress = healthCheckerProperties.getProperty("bindAddress");

    String bindPort = healthCheckerProperties.getProperty("bindPort", true);
    if (bindPort == null) {
      bindPort = String.valueOf(CallbackPortRange.SYSTEM_ASSIGNED);
    }

    this.callbackportListenerBindPort = CallbackPortRange.expandRange(bindPort);

    if (this.callbackportListenerBindPort.isEmpty()) { throw new IllegalArgumentException("No bind port(s) specified"); }
  }

  public HealthCheckerConfigClientImpl(String name, String bindPort) {
    super(name);
    this.callbackportListenerBindPort = CallbackPortRange.expandRange(bindPort);
    this.callbackportListenerBindAddress = null;
  }

  public HealthCheckerConfigClientImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                       int socketConnectMaxCount, int socketConnectTimeout, String bindAddress,
                                       String bindPort) {
    super(idle, interval, probes, name, extraCheck, socketConnectMaxCount, socketConnectTimeout);
    this.callbackportListenerBindAddress = bindAddress;
    this.callbackportListenerBindPort = CallbackPortRange.expandRange(bindPort);
  }

  @Override
  public boolean isCallbackPortListenerNeeded() {
    return true;
  }

  @Override
  public String getCallbackPortListenerBindAddress() {
    return this.callbackportListenerBindAddress;
  }

  @Override
  public Set<Integer> getCallbackPortListenerBindPort() {
    return Collections.unmodifiableSet(this.callbackportListenerBindPort);
  }

}
