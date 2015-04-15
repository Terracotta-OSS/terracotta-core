/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.transport;

import java.util.Set;

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
  public Set<Integer> getCallbackPortListenerBindPort() {
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
