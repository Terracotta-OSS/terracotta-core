/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class TerracottaToolkitCreator {

  private static final long              TIME_TO_WAIT_FOR_ASYNC_INIT                = Long
                                                                                        .parseLong(System
                                                                                            .getProperty("com.tc.non.stop.init.wait.time.millis",
                                                                                                         String
                                                                                                             .valueOf(TimeUnit.SECONDS
                                                                                                                 .toMillis(20))));

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
    final CountDownLatch latch = new CountDownLatch(1);

    Callable<ToolkitInternal> callable = new Callable<ToolkitInternal>() {
      @Override
      public ToolkitInternal call() throws Exception {
        ToolkitInternal toolkitInternal = createInternalToolkit(defaultToolkitCacheManagerProvider, true);
        latch.countDown();
        return toolkitInternal;
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

    try {
      latch.await(TIME_TO_WAIT_FOR_ASYNC_INIT, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    return futureTask;
  }

  private ToolkitInternal createInternalToolkit(Object defaultToolkitCacheManagerProvider, boolean isNonStop)
      throws Exception {
    internalClient.init();

    final String className;
    if (enterprise) {
      className = ENTERPRISE_TOOLKIT_IMPL_CLASSNAME;
    } else {
      className = TOOLKIT_IMPL_CLASSNAME;
    }

    return internalClient.instantiate(className,
                                      new Class[] { TerracottaL1Instance.class,
                                          internalClient.loadClass(TOOLKIT_DEFAULT_CM_PROVIDER), boolean.class },
                                      new Object[] { getTerracottaL1Instance(), defaultToolkitCacheManagerProvider,
                                          isNonStop });
  }

  private TerracottaInternalClient createInternalClient() {
    try {
      return TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(config);
    } catch (Exception e) {
      throw new RuntimeException(e);
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
