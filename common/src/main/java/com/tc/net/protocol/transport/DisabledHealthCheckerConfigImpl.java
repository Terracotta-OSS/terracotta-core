/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
