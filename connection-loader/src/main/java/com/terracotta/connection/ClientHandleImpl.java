/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.locks.ClientLockManager;

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
  public ClientEntityManager getClientEntityManager() {
    return client.getEntityManager();
  }
  
  @Override
  public ClientLockManager getClientLockManager() {
    return client.getLockManager();
  }
}
