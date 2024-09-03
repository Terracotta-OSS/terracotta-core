/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.object;

import com.tc.client.ClientFactory;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.TCThreadGroup;
import com.tc.net.core.ProductID;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.UUID;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionPropertyNames;

public class DistributedObjectClientFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistributedObjectClientFactory.class);

  private final Iterable<InetSocketAddress> serverAddresses;
  private final ClientBuilder builder;
  private final Properties        properties;

  public DistributedObjectClientFactory(Iterable<InetSocketAddress> serverAddresses, ClientBuilder builder,
                                        Properties properties) {
    this.serverAddresses = serverAddresses;
    this.builder = builder;
    this.properties = properties;
    Map<String, String> props = new HashMap<>();
    for (String n : properties.stringPropertyNames()) {
      if (n.startsWith("com.tc")) {
        props.put(n.substring(7), properties.getProperty(n));
      }
    }
    TCPropertiesImpl.getProperties().overwriteTcPropertiesFromConfig(props);
  }

  public DistributedObjectClient create(Runnable shutdown) throws InterruptedException, TimeoutException {
    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(LoggerFactory.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {
                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     return null;
                                                                   }
                                                                 });
    String uuid = this.properties.getProperty(ConnectionPropertyNames.CONNECTION_UUID, UUID.getUUID().toString());
    String name = this.properties.getProperty(ConnectionPropertyNames.CONNECTION_NAME, "");
    final TCThreadGroup group = new TCThreadGroup(throwableHandler, name + "/" + uuid, true);

    DistributedObjectClient client = ClientFactory.createClient(serverAddresses, builder, group, uuid, name);
    client.addShutdownHook(shutdown);
    
    //  weak reference to make sure threads associated with the client aren't preventing 
    //  garbage collection of the client
    Reference<DistributedObjectClient> ref = new WeakReference<>(client);
    
    throwableHandler.addCallbackOnExitDefaultHandler(state->{
      DistributedObjectClient err = ref.get();
      LOGGER.error("FATAL error in the client", state.getThrowable());
      if (err != null) {
        err.dump();
        err.shutdown();
      }
    });
    
    ProductID type = builder.getTypeOfClient();
    boolean reconnect = !type.isReconnectEnabled();
    String timeout = properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "0");
    if (reconnect && Integer.parseInt(timeout) < 0) {
      if (!client.connectOnce()) {
        return null;
      }
    } else {
      if (!client.connectFor(Long.parseLong(timeout), TimeUnit.MILLISECONDS)) {
//  timed out, shutdown the extra threads and return null;
        LOGGER.warn("connection timeout {}", this);
        throw new TimeoutException("connection timeout in " + timeout);
      }
    }

//  something serious happened, try to shutdown extran threads and throw
    return client;
  }
}
