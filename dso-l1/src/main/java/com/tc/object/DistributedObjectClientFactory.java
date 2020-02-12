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
import com.tc.util.UUID;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionPropertyNames;

public class DistributedObjectClientFactory {
  private final Iterable<InetSocketAddress> serverAddresses;
  private final ClientBuilder builder;
  private final Properties        properties;

  public DistributedObjectClientFactory(Iterable<InetSocketAddress> serverAddresses, ClientBuilder builder,
                                        Properties properties) {
    this.serverAddresses = serverAddresses;
    this.builder = builder;
    this.properties = properties;
  }

  public DistributedObjectClient create() throws InterruptedException, ConfigurationSetupException {
    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(LoggerFactory.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {
                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     return null;
                                                                   }
                                                                 });
    String uuid = this.properties.getProperty(ConnectionPropertyNames.CONNECTION_UUID, UUID.getUUID().toString());
    String name = this.properties.getProperty(ConnectionPropertyNames.CONNECTION_NAME, "");
    final TCThreadGroup group = new TCThreadGroup(throwableHandler, name + "/" + uuid);
    boolean async = Boolean.parseBoolean(this.properties.getProperty(ConnectionPropertyNames.CONNECTION_ASYNC, "false"));
    
    DistributedObjectClient client = ClientFactory.createClient(serverAddresses, builder, group, uuid, name, async);

    Reference<DistributedObjectClient> ref = new WeakReference<>(client);
    group.addCallbackOnExitDefaultHandler((state)->{
      DistributedObjectClient ce = ref.get();
      if (ce != null) {
        ce.dump();
        ce.shutdown();
      }
    });
    
    try {
      client.start();
      String timeout = properties.getProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "0");
      if (!client.waitForConnection(Long.parseLong(timeout), TimeUnit.MILLISECONDS)) {
//  timed out, shutdown the extra threads and return null;
        client.shutdown();
        return null;
      }
    } catch (InterruptedException ie) {
// wait got interrupted, shutdown extra threads and return nothing
      client.shutdown();
      return null;
    } catch (RuntimeException exp) {
//  something serious happened, try to shutdown extran threads and throw
      client.shutdown();
      throw exp;
    } catch (Error e) {
//  something serious happened, try to shutdown extran threads and throw
      client.shutdown();
      throw e;
    }
    return client;
  }
}
