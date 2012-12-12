/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class TerracottaToolkitCreator {

  private static final String            TOOLKIT_IMPL_CLASSNAME                     = "com.terracotta.toolkit.TerracottaToolkit";
  private static final String            ENTERPRISE_TOOLKIT_IMPL_CLASSNAME          = "com.terracotta.toolkit.EnterpriseTerracottaToolkit";

  private static final String            NON_STOP_TOOLKIT_IMPL_CLASSNAME            = "com.terracotta.toolkit.NonStopToolkitImpl";
  private static final String            ENTERPRISE_NON_STOP_TOOLKIT_IMPL_CLASSNAME = "com.terracotta.toolkit.EnterpriseNonStopToolkitImpl";

  private static final String            TOOLKIT_DEFAULT_CM_PROVIDER                = "com.terracotta.toolkit.ToolkitCacheManagerProvider";

  private static final String            PLATFORM_SERVICE                           = "com.tc.platform.PlatformService";

  private final TerracottaInternalClient internalClient;
  private final boolean                  enterprise;
  private final TerracottaClientConfig   config;

  public TerracottaToolkitCreator(TerracottaClientConfig config, boolean enterprise) {
    this.enterprise = enterprise;
    if (config == null) { throw new NullPointerException("terracottaClientConfig cannot be null"); }
    this.config = config;
    internalClient = createInternalClient();
  }

  public ToolkitInternal createToolkit() {
    try {
      final Object defaultToolkitCacheManagerProvider = initializeDefaultCacheManagerProvider();
      if (config.isNonStopEnabled()) {
        final FutureTask<ToolkitInternal> futureTask = createInternalToolkitAsynchronously(defaultToolkitCacheManagerProvider);
        return instantiateNonStopToolkit(futureTask);
      } else {
        return createInternalToolkit(defaultToolkitCacheManagerProvider, false);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to create toolkit.", e);
    }
  }

  private FutureTask<ToolkitInternal> createInternalToolkitAsynchronously(final Object defaultToolkitCacheManagerProvider) {
    Callable<ToolkitInternal> callable = new Callable<ToolkitInternal>() {
      @Override
      public ToolkitInternal call() throws Exception {
        return createInternalToolkit(defaultToolkitCacheManagerProvider, true);
      }
    };
    final FutureTask<ToolkitInternal> futureTask = new FutureTask<ToolkitInternal>(callable);
    Thread t = new Thread("Non Stop initialization of Toolkit") {
      @Override
      public void run() {
        futureTask.run();
      }
    };
    t.start();
    return futureTask;
  }

  private ToolkitInternal createInternalToolkit(Object defaultToolkitCacheManagerProvider, boolean isNonStop)
      throws Exception {
    internalClient.init();

    if (enterprise) {
      return internalClient.instantiate(ENTERPRISE_TOOLKIT_IMPL_CLASSNAME, new Class[] { TerracottaL1Instance.class,
          internalClient.loadClass(TOOLKIT_DEFAULT_CM_PROVIDER), Boolean.class }, new Object[] {
          getTerracottaL1Instance(), defaultToolkitCacheManagerProvider, isNonStop });
    } else {
      return internalClient.instantiate(TOOLKIT_IMPL_CLASSNAME, new Class[] { TerracottaL1Instance.class,
          internalClient.loadClass(TOOLKIT_DEFAULT_CM_PROVIDER) }, new Object[] { getTerracottaL1Instance(),
          defaultToolkitCacheManagerProvider });
    }
  }

  private TerracottaInternalClient createInternalClient() {
    try {
      return TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(config);
    } catch (Exception e) {
      throw new RuntimeException("Unable to create Terracotta L1 Client", e);
    }
  }

  private ToolkitInternal instantiateNonStopToolkit(FutureTask<ToolkitInternal> futureTask) throws Exception {
    String className;
    if (enterprise) {
      className = ENTERPRISE_NON_STOP_TOOLKIT_IMPL_CLASSNAME;
    } else {
      className = NON_STOP_TOOLKIT_IMPL_CLASSNAME;
    }

    return internalClient.instantiate(className,
                                      new Class[] { FutureTask.class, internalClient.loadClass(PLATFORM_SERVICE) },
                                      new Object[] { futureTask, internalClient.getPlatformService() });
  }

  private TerracottaL1Instance getTerracottaL1Instance() {
    return new TCL1Instance(internalClient);
  }

  private static class TCL1Instance implements TerracottaL1Instance {

    private final TerracottaInternalClient terracottaInternalClient;

    public TCL1Instance(TerracottaInternalClient terracottaInternalClient) {
      this.terracottaInternalClient = terracottaInternalClient;
    }

    @Override
    public void shutdown() {
      terracottaInternalClient.shutdown();
    }

  }

  public Object initializeDefaultCacheManagerProvider() {
    try {
      return internalClient.instantiate(TOOLKIT_DEFAULT_CM_PROVIDER, null, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
