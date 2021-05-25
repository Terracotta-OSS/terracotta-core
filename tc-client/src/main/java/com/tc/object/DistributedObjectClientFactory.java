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
package com.tc.object;

import com.tc.client.ClientFactory;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.TCThreadGroup;
import com.tc.net.core.ProductID;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.UUID;
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

  public DistributedObjectClient create(Runnable shutdown) throws InterruptedException, ConfigurationSetupException, TimeoutException {
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
    boolean async = Boolean.parseBoolean(this.properties.getProperty(ConnectionPropertyNames.CONNECTION_ASYNC, "false"));
    
    DistributedObjectClient client = ClientFactory.createClient(serverAddresses, builder, group, uuid, name, async);
    client.addShutdownHook(shutdown);
    
    ProductID type = builder.getTypeOfClient();
    boolean reconnect = !type.isReconnectEnabled();
    String timeout = properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "0");
    if (reconnect && Integer.parseInt(timeout) < 0) {
      if (!client.connectOnce(5_000)) {
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
