/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;


public class HealthCheckerConfigClientImpl extends HealthCheckerConfigImpl {

  public HealthCheckerConfigClientImpl(TCProperties healthCheckerProperties, String hcName) {
    super(healthCheckerProperties, hcName);
  }

  public HealthCheckerConfigClientImpl(String name, String bindPort) {
    super(name);
  }

  public HealthCheckerConfigClientImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                       int socketConnectMaxCount, int socketConnectTimeout) {
    super(idle, interval, probes, name, extraCheck, socketConnectMaxCount, socketConnectTimeout);
  }

  @Override
  public boolean isCallbackPortListenerNeeded() {
    return true;
  }
}
