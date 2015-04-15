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
package com.terracotta.toolkit.express;

import com.tc.object.DistributedObjectClient;

import java.util.Set;

public class ClientHandleImpl implements ClientHandle {

  private final DistributedObjectClient client;

  public ClientHandleImpl(Object client) {
    this.client = (DistributedObjectClient) client;
  }

  @Override
  public void activateTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains) {
    boolean sendCurrentTunnelledDomains = false;
    if (tunnelledMBeanDomains != null) {
      for (String mbeanDomain : tunnelledMBeanDomains) {
        client.addTunneledMBeanDomain(mbeanDomain);
        sendCurrentTunnelledDomains = true;
      }
    }
    if (sendCurrentTunnelledDomains) {
      client.getTunneledDomainManager().sendCurrentTunneledDomains();
    }
  }

  @Override
  public void shutdown() {
    client.shutdown();
  }

  @Override
  public boolean isOnline() {
    return client.getPlatformService().getDsoCluster().areOperationsEnabled();
  }

  @Override
  public Object getPlatformService() {
    return client.getPlatformService();
  }
}
