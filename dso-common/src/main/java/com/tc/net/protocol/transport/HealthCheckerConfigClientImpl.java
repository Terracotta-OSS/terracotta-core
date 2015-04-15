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
