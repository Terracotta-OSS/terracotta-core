/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Default Health Chcker config passed to the Communications Manager
 * 
 * @author Manoj
 */
public class DisabledHealthCheckerConfigImpl implements HealthCheckerConfig {

  public boolean isHealthCheckerEnabled() {
    return false;
  }

  public long getPingIdleTimeMillis() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public long getPingIntervalMillis() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public int getPingProbes() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public String getHealthCheckerName() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public boolean isSocketConnectOnPingFail() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public int getSocketConnectMaxCount() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public int getSocketConnectTimeout() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public String getCallbackPortListenerBindAddress() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public int getCallbackPortListenerBindPort() {
    throw new AssertionError("Disabled HealthChecker");
  }

  public boolean isCallbackPortListenerNeeded() {
    return false;
  }

}
