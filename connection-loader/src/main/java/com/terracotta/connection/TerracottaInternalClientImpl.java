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

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.core.SecurityInfo;
import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.object.StandardClientBuilder;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.terracotta.connection.ConnectionPropertyNames;


public class TerracottaInternalClientImpl implements TerracottaInternalClient {
  static class ClientShutdownException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private final DistributedObjectClientFactory clientCreator;
  private volatile ClientHandle       clientHandle;
  private volatile boolean            shutdown             = false;
  private volatile boolean            isInitialized        = false;

  TerracottaInternalClientImpl(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Properties props) {
    try {
      this.clientCreator = buildClientCreator(stripeConnectionConfig, props);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private DistributedObjectClientFactory buildClientCreator(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Properties props) {
    boolean noreconnect = Boolean.valueOf(props.getProperty(ConnectionPropertyNames.CONNECTION_DISABLE_RECONNECT, "false"));  // TODO: replace with ConnectionPropertyNames.CONNECTION_DISABLE_RECONNECT once API is released
    return new DistributedObjectClientFactory(stripeConnectionConfig.getStripeMemberUris(),
         new StandardClientBuilder(noreconnect), 
         null,  // no security features
         new SecurityInfo(false, null),  // no security info
         props);
  }

  @Override
  public synchronized void init() throws TimeoutException, InterruptedException, ConfigurationSetupException {
    if (isInitialized) { return; }

    DistributedObjectClient client = clientCreator.create();
    if (client != null) {
      clientHandle = new ClientHandleImpl(client);
      isInitialized = true;
    } else {
      throw new TimeoutException();
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
}
