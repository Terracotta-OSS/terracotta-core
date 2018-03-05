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

import com.tc.object.ClientBuilder;
import com.tc.object.ClientBuilderFactory;
import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.net.protocol.transport.ClientConnectionErrorDetails;
import com.terracotta.connection.api.DetailedConnectionException;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.util.Properties;
import java.util.concurrent.TimeoutException;


public class TerracottaInternalClientImpl implements TerracottaInternalClient {
  static class ClientShutdownException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  private final DistributedObjectClientFactory clientCreator;
  private final ClientConnectionErrorDetails errorListener = new ClientConnectionErrorDetails();
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
    ClientBuilder clientBuilder = ClientBuilderFactory.get().create(props);
    clientBuilder.setClientConnectionErrorListener(errorListener);
    return new DistributedObjectClientFactory(stripeConnectionConfig.getStripeMemberUris(), clientBuilder, props);
  }

  @Override
  public synchronized void init() throws DetailedConnectionException {
    if (isInitialized) { return; }
    DistributedObjectClient client = null;
    //Attach the internal collector in the listener
    errorListener.attachCollector();
    try {
      try {
        client = clientCreator.create();
      } catch (Exception e){
        throw new DetailedConnectionException(new Exception(DetailedConnectionException.getDetailedMessage(errorListener.getErrors()), e), errorListener.getErrors());
      }
      if (client != null) {
        clientHandle = new ClientHandleImpl(client);
        isInitialized = true;
      } else {
        TimeoutException e = new TimeoutException(DetailedConnectionException.getDetailedMessage(errorListener.getErrors()));
        throw new DetailedConnectionException(e, errorListener.getErrors());
      }
    } finally {
      /*
      Remove the internal collector. if connection is made fine (i.e. clientCreator.create() returns non null), we do 
      not to need to track the exceptions. If exception is thrown from invocation or clientCreator.create() is returned
      null, then also internal collector should be detached. In both the cases, errorListener.getErrors() is called and
      which effectively gives a copy of internal collector. After getting the copies, it makes sense to clean the 
      internal collector. 
       */
      errorListener.removeCollector();
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
