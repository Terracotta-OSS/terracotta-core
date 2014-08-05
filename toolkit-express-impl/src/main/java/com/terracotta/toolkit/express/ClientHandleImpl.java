/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
