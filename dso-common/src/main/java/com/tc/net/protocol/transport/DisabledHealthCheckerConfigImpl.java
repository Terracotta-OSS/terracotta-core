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

  @Override
  public boolean isHealthCheckerEnabled() {
    return false;
  }

  @Override
  public long getPingIdleTimeMillis() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public long getPingIntervalMillis() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public int getPingProbes() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public String getHealthCheckerName() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public boolean isSocketConnectOnPingFail() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public int getSocketConnectMaxCount() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public int getSocketConnectTimeout() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public String getCallbackPortListenerBindAddress() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public int getCallbackPortListenerBindPort() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public boolean isCallbackPortListenerNeeded() {
    return false;
  }

  @Override
  public boolean isCheckTimeEnabled() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public long getCheckTimeInterval() {
    throw new AssertionError("Disabled HealthChecker");
  }

  @Override
  public long getTimeDiffThreshold() {
    throw new AssertionError("Disabled HealthChecker");
  }

}
