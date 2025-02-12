/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.terracotta.connection;

import com.tc.object.ClientBuilder;
import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.net.protocol.transport.ClientConnectionErrorDetails;
import com.tc.object.StandardClientBuilderFactory;
import com.tc.util.concurrent.SetOnceFlag;
import com.terracotta.connection.api.DetailedConnectionException;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.terracotta.exception.ConnectionClosedException;


public class TerracottaInternalClientImpl implements TerracottaInternalClient {
  private final DistributedObjectClientFactory clientCreator;
  private final ClientConnectionErrorDetails errorListener = new ClientConnectionErrorDetails();
  private volatile ClientHandle       clientHandle;
  private SetOnceFlag           isInitialized        = new SetOnceFlag();

  TerracottaInternalClientImpl(String scheme, Iterable<InetSocketAddress> serverAddresses, Properties props) {
    try {
      this.clientCreator = buildClientCreator(scheme, serverAddresses, props);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private DistributedObjectClientFactory buildClientCreator(String scheme, Iterable<InetSocketAddress> serverAddresses,
                                                            Properties props) {
    ClientBuilder clientBuilder = new StandardClientBuilderFactory(scheme).create(props);
    if (clientBuilder == null) {
      throw new RuntimeException("unable to build the client");
    }
    clientBuilder.setClientConnectionErrorListener(errorListener);
    return new DistributedObjectClientFactory(serverAddresses, clientBuilder, props);
  }

  @Override
  public void init() throws DetailedConnectionException {
    if (!isInitialized.attemptSet()) { return; }
    DistributedObjectClient client = null;
    //Attach the internal collector in the listener
    errorListener.attachCollector();
    try {
      try {
        client = clientCreator.create(this::destroy);
      } catch (TimeoutException | InterruptedException to) {
        throw new DetailedConnectionException(to, errorListener.getErrors());
      } catch (RuntimeException e){
        throw new DetailedConnectionException(new Exception(DetailedConnectionException.getDetailedMessage(errorListener.getErrors()), e), errorListener.getErrors());
      }
      if (client != null) {
        clientHandle = new ClientHandleImpl(client);
      } else {
        throw new DetailedConnectionException(new IOException("Connection refused"), errorListener.getErrors());
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
  public boolean isShutdown() {
    try {
      return getHandle().isShutdown();
    } catch (Throwable t) {
      if (containsCause(t, ConnectionClosedException.class)) {
        return true;
      } else {
        throw t;
      }
    }
  }






  @Override
  public void shutdown() {
    try {
      getHandle().shutdown();
    } catch (Throwable t) {
      if (!containsCause(t, ConnectionClosedException.class)) {
        throw t;
      }
    }
  }

  private boolean containsCause(Throwable failure, Class<? extends Throwable> cause) {
    Throwable intermediate = failure;

    while(!cause.isInstance(intermediate)) {
      if ((intermediate = intermediate.getCause()) == null) {
        return false;
      }
    }
    return true;
  }


// connection destroyed from below
  private void destroy() {
    clientHandle = null;
  }

  private ClientHandle getHandle() {
    ClientHandle handle = clientHandle;
    if (handle == null) {
      throw new ConnectionClosedException("connection delared dead");
    } else {
      return handle;
    }
  }
  
  @Override
  public ClientEntityManager getClientEntityManager() {
    try {
      return clientHandle.getClientEntityManager();
    } catch (NullPointerException n) {
      throw new ConnectionClosedException("connection delared dead");
    }
  }
}
