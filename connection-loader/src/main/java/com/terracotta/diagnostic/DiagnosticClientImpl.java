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
package com.terracotta.diagnostic;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.core.SecurityInfo;
import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.util.ProductID;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.util.Properties;
import java.util.concurrent.TimeoutException;


public class DiagnosticClientImpl implements TerracottaInternalClient {
  static class ClientShutdownException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private final DistributedObjectClientFactory clientCreator;
  private volatile DistributedObjectClient       client;
  private volatile boolean            shutdown             = false;
  private volatile boolean            isInitialized        = false;

  public DiagnosticClientImpl(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Properties properties) {
    try {
      this.clientCreator = buildClientCreator(stripeConnectionConfig, properties);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private DistributedObjectClientFactory buildClientCreator(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Properties props) {
    ProductID productId = ProductID.DIAGNOSTIC;
    return new DistributedObjectClientFactory(stripeConnectionConfig.getStripeMemberUris(),
         new DiagnosticClientBuilder(),
         null,  // no security features
         new SecurityInfo(false, null),  // no security info
         productId,
         props, true);
  }

  public synchronized void init() throws TimeoutException, InterruptedException, ConfigurationSetupException {
    if (isInitialized) { return; }

    DistributedObjectClient c = clientCreator.create();
    if (c != null) {
      this.client = c;
    } else {
      throw new TimeoutException();
    }
  }

  public synchronized boolean isShutdown() {
    return shutdown;
  }

  public synchronized void shutdown() {
    shutdown = true;
    try {
      if (this.client != null) {
        client.shutdown();
      }
    } finally {
      client = null;
    }
  }

  public boolean isInitialized() {
    return isInitialized;
  }
  
  @Override
  public ClientEntityManager getClientEntityManager() {
    return this.client.getEntityManager();
  }
  
}
