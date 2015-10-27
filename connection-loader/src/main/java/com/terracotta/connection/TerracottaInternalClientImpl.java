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
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;


public class TerracottaInternalClientImpl implements TerracottaInternalClient {
  static class ClientShutdownException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private final ClientCreatorCallable clientCreator;
  private final Set<String>           tunneledMBeanDomains = new HashSet<String>();
  private volatile ClientHandle       clientHandle;
  private volatile boolean            shutdown             = false;
  private volatile boolean            isInitialized        = false;

  TerracottaInternalClientImpl(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Set<String> tunneledMBeanDomains, String productId) {
    if (tunneledMBeanDomains != null) {
      this.tunneledMBeanDomains.addAll(tunneledMBeanDomains);
    }
    try {
      Callable<ClientCreatorCallable> boot = new CreateClient(stripeConnectionConfig, productId);
      this.clientCreator = boot.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void init() {
    if (isInitialized) { return; }

    try {
      clientHandle = new ClientHandleImpl(clientCreator.call());

      isInitialized = true;
      join(tunneledMBeanDomains);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public String getUuid() {
    return clientCreator.getUuid();
  }

  private synchronized void join(Set<String> tunnelledMBeanDomainsParam) throws ClientShutdownException {
    if (shutdown) throw new ClientShutdownException();

    if (isInitialized) {
      clientHandle.activateTunnelledMBeanDomains(tunnelledMBeanDomainsParam);
    } else {
      if (tunnelledMBeanDomainsParam != null) {
        this.tunneledMBeanDomains.addAll(tunnelledMBeanDomainsParam);
      }
    }
  }

  @Override
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  @Override
  public synchronized void shutdown() {
    shutdown = true;
    try {
      if (clientHandle != null) {
        clientHandle.shutdown();
      }
    } finally {
      clientHandle = null;
    }
  }

  @Override
  public boolean isInitialized() {
    return isInitialized;
  }
  
  @Override
  public ClientEntityManager getClientEntityManager() {
    return clientHandle.getClientEntityManager();
  }
  
  @Override
  public ClientLockManager getClientLockManager() {
    return clientHandle.getClientLockManager();
  }
}
