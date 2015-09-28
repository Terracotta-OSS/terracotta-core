/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.client.ClientFactory;
import com.tc.cluster.ClusterImpl;
import com.tc.config.schema.setup.ClientConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.ClientConfigImpl;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.util.UUID;
import com.tcclient.cluster.ClusterInternal;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class DistributedObjectClientFactory {

  private final String            configSpec;
  private final TCSecurityManager securityManager;
  private final SecurityInfo      securityInfo;
  private final ProductID         productId;
  private final UUID                      uuid;

  public DistributedObjectClientFactory(String configSpec, TCSecurityManager securityManager,
                                        SecurityInfo securityInfo, 
                                        ProductID productId,
                                        UUID uuid) {
    this.configSpec = configSpec;
    this.securityManager = securityManager;
    this.securityInfo = securityInfo;
    this.productId = productId;
    this.uuid = uuid;
  }

  public DistributedObjectClient create() throws ConfigurationSetupException {
    final AtomicReference<DistributedObjectClient> clientRef = new AtomicReference<DistributedObjectClient>();

    ClientConfigurationSetupManagerFactory factory = new ClientConfigurationSetupManagerFactory(null, configSpec, securityManager);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager(securityInfo);
    config.setupLogging();

    final PreparedComponentsFromL2Connection connectionComponents;
    try {
      connectionComponents = validateMakeL2Connection(config);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    final ClientConfig configHelper = new ClientConfigImpl(config);
    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {

                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     clientRef.get().shutdown();
                                                                     return null;
                                                                   }
                                                                 });
    final TCThreadGroup group = new TCThreadGroup(throwableHandler);

    final ClusterInternal cluster = new ClusterImpl();

    final StartupAction action = new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        DistributedObjectClient client = ClientFactory.createClient(configHelper, group, connectionComponents, cluster, securityManager,
            uuid,
            productId);

        client.start();

        cluster.init(client.getClusterEventsStage());
        clientRef.set(client);
      }
    };

    final StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();

    return clientRef.get();
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1ConfigurationSetupManager config) {
    return new PreparedComponentsFromL2Connection(config);
  }

}
