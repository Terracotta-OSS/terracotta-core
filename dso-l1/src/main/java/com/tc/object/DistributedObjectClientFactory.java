/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortableOperationManager;
import com.tc.client.AbstractClientFactory;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.SingleLoaderClassProvider;
import com.tc.object.tx.ClusterEventListener;
import com.tc.platform.rejoin.RejoinManagerImpl;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tcclient.cluster.DsoClusterInternal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class DistributedObjectClientFactory {

  private final String            configSpec;
  private final TCSecurityManager securityManager;
  private final SecurityInfo      securityInfo;
  private final ClassLoader       loader;
  private final boolean           rejoin;
  private final ProductID         productId;
  private final AbortableOperationManager abortableOperationManager;
  private final UUID                      uuid;

  public static TCSecurityManager createSecurityManager(Map<String, Object> env) {
    return AbstractClientFactory.getFactory().createClientSecurityManager(env);
  }

  public DistributedObjectClientFactory(String configSpec, TCSecurityManager securityManager,
                                        SecurityInfo securityInfo, ClassLoader loader, boolean rejoin,
                                        ProductID productId, AbortableOperationManager abortableOperationManager,
                                        UUID uuid) {
    this.configSpec = configSpec;
    this.securityManager = securityManager;
    this.securityInfo = securityInfo;
    this.loader = loader;
    this.rejoin = rejoin;
    this.productId = productId;
    this.abortableOperationManager = abortableOperationManager;
    this.uuid = uuid;
  }

  public DistributedObjectClient create() throws ConfigurationSetupException {
    final AtomicReference<DistributedObjectClient> clientRef = new AtomicReference<DistributedObjectClient>();

    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    (String[]) null,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.EXPRESS_L1,
                                                                                                    new FatalIllegalConfigurationChangeHandler(),
                                                                                                    configSpec,
                                                                                                    securityManager);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager(securityInfo);
    config.setupLogging();

    final PreparedComponentsFromL2Connection connectionComponents;
    try {
      connectionComponents = validateMakeL2Connection(config, securityManager);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    final DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(config);

    try {
      startToolkitConfigurator(configHelper);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {

                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     clientRef.get().shutdown();
                                                                     return null;
                                                                   }
                                                                 });
    final TCThreadGroup group = new TCThreadGroup(throwableHandler);

    final ClassProvider classProvider = new SingleLoaderClassProvider(
                                                                      loader == null ? DistributedObjectClientFactory.class
                                                                          .getClassLoader() : loader);

    final RejoinManagerInternal rejoinManager = new RejoinManagerImpl(rejoin);
    final DsoClusterInternal dsoCluster = new DsoClusterImpl(rejoinManager);

    final StartupAction action = new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        final AbstractClientFactory clientFactory = AbstractClientFactory.getFactory();
        DistributedObjectClient client = clientFactory.createClient(configHelper, group, classProvider,
                                                                    connectionComponents, dsoCluster, securityManager,
                                                                    abortableOperationManager, rejoinManager, uuid,
                                                                    productId);

        client.start();

        dsoCluster.init(client.getClusterMetaDataManager(), client.getObjectManager(), client.getClusterEventsStage());
        dsoCluster.addClusterListener(new ClusterEventListener(client.getRemoteTransactionManager()));

        clientRef.set(client);
      }
    };

    final StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();

    return clientRef.get();
  }

  private static void startToolkitConfigurator(DSOClientConfigHelper configHelper) throws Exception {
    Class toolkitConfiguratorClass = null;
    try {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.EnterpriseToolkitConfigurator");
    } catch (ClassNotFoundException e) {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.ToolkitConfigurator");
    }

    Object toolkitConfigurator = toolkitConfiguratorClass.newInstance();
    Method start = toolkitConfiguratorClass.getMethod("start", DSOClientConfigHelper.class);
    start.invoke(toolkitConfigurator, configHelper);
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1ConfigurationSetupManager config,
                                                                             final TCSecurityManager securityManager) {
    L2Data[] l2Data = config.l2Config().l2Data();
    Assert.assertNotNull(l2Data);

    return new PreparedComponentsFromL2Connection(config, securityManager);
  }

}
