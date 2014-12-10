/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class TerracottaToolkitCreator {

  private static final long                  TIME_TO_WAIT_FOR_ASYNC_INIT                = Long
                                                                                            .getLong("com.tc.non.stop.init.wait.time.millis",
                                                                                                     TimeUnit.SECONDS
                                                                                                         .toMillis(20));
  private static final String                INIT_WAIT_KEY                              = "toolkit.init.wait.time.millis";
  private static final String                NON_STOP_INIT_THREAD_NAME                  = "Non Stop initialization of Toolkit";
  private static final String                INIT_THREAD_NAME                           = "Initialization of Toolkit";
  private static final String                TOOLKIT_IMPL_CLASSNAME                     = "com.terracotta.toolkit.TerracottaToolkit";
  private static final String                ENTERPRISE_TOOLKIT_IMPL_CLASSNAME          = "com.terracotta.toolkit.EnterpriseTerracottaToolkit";

  private static final String                NON_STOP_TOOLKIT_IMPL_CLASSNAME            = "com.terracotta.toolkit.NonStopToolkitImpl";
  private static final String                ENTERPRISE_NON_STOP_TOOLKIT_IMPL_CLASSNAME = "com.terracotta.toolkit.EnterpriseNonStopToolkitImpl";

  private static final String                TOOLKIT_DEFAULT_CM_PROVIDER                = "com.terracotta.toolkit.ToolkitCacheManagerProvider";

  private static final String                PLATFORM_SERVICE                           = "com.tc.platform.PlatformService";
  private static final String                ABORTABLE_OPERATION_MANAGER                = "com.tc.abortable.AbortableOperationManager";

  private final TerracottaInternalClient     internalClient;
  private final boolean                      enterprise;
  private final TerracottaClientConfig       config;
  private final Properties                   toolkitProperties;
  private final ClassLoader                  loader;
  private final boolean                      isNonStop;
  private final NonStopPlatformServiceHelper nonStopPlatformServiceHelper;
  private final boolean                      nonstopInitEnabled;

  public TerracottaToolkitCreator(TerracottaClientConfig config, Properties properties, boolean enterprise) {
    this.enterprise = enterprise;
    if (config == null) { throw new NullPointerException("terracottaClientConfig cannot be null"); }
    this.config = config;
    this.internalClient = createInternalClient();
    this.toolkitProperties = properties;
    this.loader = config.getClassLoader();
    this.isNonStop = config.isNonStopEnabled();
    this.nonstopInitEnabled = config.isAsyncInit();
    this.nonStopPlatformServiceHelper = new NonStopPlatformServiceHelper();
  }

  public ToolkitInternal createToolkit() {
    try {
      final Object defaultToolkitCacheManagerProvider = initializeDefaultCacheManagerProvider();
      if (!nonstopInitEnabled) {
        if (isNonStop) return instantiateNonStopToolkit(createInternalToolkitSynchronously(defaultToolkitCacheManagerProvider));
        else return createInternalToolkitSynchronously(defaultToolkitCacheManagerProvider);
      }
      FutureTask<ToolkitInternal> futureTask;
      if (isNonStop) {
        futureTask = createInternalToolkitAsynchronously(defaultToolkitCacheManagerProvider, NON_STOP_INIT_THREAD_NAME,
                                                         TIME_TO_WAIT_FOR_ASYNC_INIT);

        return instantiateNonStopToolkit(futureTask);
      } else {
        final long timeout = getToolkitInitTimeout();
        futureTask = createInternalToolkitAsynchronously(defaultToolkitCacheManagerProvider, INIT_THREAD_NAME, timeout);
        if (!futureTask.isDone()) { throw new ToolkitInstantiationException("Not able to initialize toolkit in "
                                                                            + timeout + " milliseconds"); }
        return futureTask.get();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to create toolkit.", e);
    }
  }

  private long getToolkitInitTimeout() throws ToolkitInstantiationException {
    long timeoutInMills = Long.valueOf(toolkitProperties.getProperty(INIT_WAIT_KEY, String.valueOf(Long.MAX_VALUE)));
    if (timeoutInMills <= 0) { throw new ToolkitInstantiationException(
                                                                       "Toolkit initilization timeout should be greater than zero but provided "
                                                                           + timeoutInMills); }
    return timeoutInMills;
  }

  private ToolkitInternal createInternalToolkitSynchronously(final Object defaultToolkitCacheManagerProvider)
      throws Exception {
    ToolkitInternal toolkit = null;
    try {
      toolkit = createInternalToolkit(defaultToolkitCacheManagerProvider);
    } catch (Exception e) {
      if (defaultToolkitCacheManagerProvider != null) {
        NonStopPlatformServiceHelper.getMethod(defaultToolkitCacheManagerProvider, "shutdownDefaultCacheManager")
            .invoke(defaultToolkitCacheManagerProvider);
      }
      throw e;
    }
    if (isNonStop) nonStopPlatformServiceHelper.setPlatformService(internalClient.getPlatformService());
    return toolkit;

  }

  private FutureTask<ToolkitInternal> createInternalToolkitAsynchronously(final Object defaultToolkitCacheManagerProvider,
                                                                          String threadName, long timeoutInMills) {
    final CountDownLatch latch = new CountDownLatch(1);

    Callable<ToolkitInternal> callable = new Callable<ToolkitInternal>() {
      @Override
      public ToolkitInternal call() throws Exception {
        return createInternalToolkitSynchronously(defaultToolkitCacheManagerProvider);
      }
    };
    final FutureTask<ToolkitInternal> futureTask = new FutureTask<ToolkitInternal>(callable);
    Thread t = new Thread(threadName) {
      @Override
      public void run() {
        futureTask.run();
        latch.countDown();
      }
    };
    t.start();

    try {
      latch.await(timeoutInMills, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    return futureTask;
  }

  private ToolkitInternal createInternalToolkit(Object defaultToolkitCacheManagerProvider) throws Exception {
    internalClient.init();

    final String className;
    if (enterprise) {
      className = ENTERPRISE_TOOLKIT_IMPL_CLASSNAME;
    } else {
      className = TOOLKIT_IMPL_CLASSNAME;
    }

    return internalClient.instantiate(className,
                                      new Class[] { TerracottaL1Instance.class,
                                          internalClient.loadClass(TOOLKIT_DEFAULT_CM_PROVIDER), boolean.class,
                                          ClassLoader.class, internalClient.loadClass(PLATFORM_SERVICE) },
                                      new Object[] { getTerracottaL1Instance(), defaultToolkitCacheManagerProvider,
                                          isNonStop, loader, internalClient.getPlatformService() });
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

    ToolkitInternal tk = internalClient.instantiate(className,
                                                    new Class[] { FutureTask.class,
                                                        internalClient.loadClass(ABORTABLE_OPERATION_MANAGER),
                                                        String.class },
                                                    new Object[] { futureTask,
                                                        internalClient.getAbortableOperationManager(),
                                                        internalClient.getUuid() });

    nonStopPlatformServiceHelper.setToolkit(tk);
    return tk;
  }

  private ToolkitInternal instantiateNonStopToolkit(ToolkitInternal toolkit) throws Exception {
    String className;
    if (enterprise) {
      className = ENTERPRISE_NON_STOP_TOOLKIT_IMPL_CLASSNAME;
    } else {
      className = NON_STOP_TOOLKIT_IMPL_CLASSNAME;
    }

    ToolkitInternal tk = internalClient.instantiate(className,
                                                    new Class[] { ToolkitInternal.class,
                                                        internalClient.loadClass(ABORTABLE_OPERATION_MANAGER),
                                                        String.class },
                                                    new Object[] { toolkit,
                                                        internalClient.getAbortableOperationManager(),
                                                        internalClient.getUuid() });

    nonStopPlatformServiceHelper.setToolkit(tk);
    return tk;
  }

  private TerracottaL1Instance getTerracottaL1Instance() {
    return new TCL1Instance(internalClient);
  }

  // Sets the platform service on the toolkit when both it and the toolkit instance are available (they are created in
  // concurrent threads)
  private static class NonStopPlatformServiceHelper {
    private boolean         set = false;
    private Object          platformService;
    private ToolkitInternal nonStopToolkit;

    synchronized void setPlatformService(Object platformService) throws Exception {
      this.platformService = platformService;
      attemptSet();
    }

    synchronized void setToolkit(ToolkitInternal nonStopToolkit) throws Exception {
      this.nonStopToolkit = nonStopToolkit;
      attemptSet();
    }

    private void attemptSet() throws Exception {
      if (set) return;

      if (platformService != null && nonStopToolkit != null) {
        set = true;
        getMethod(nonStopToolkit, "setPlatformService").invoke(nonStopToolkit, platformService);
      }
    }

    private static Method getMethod(Object obj, String name) {
      for (Method m : obj.getClass().getMethods()) {
        if (m.getName().equals(name)) { return m; }
      }
      throw new AssertionError("No accessible method [" + name + "] on " + obj.getClass());
    }

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

  private Object initializeDefaultCacheManagerProvider() {
    try {
      return internalClient.instantiate(TOOLKIT_DEFAULT_CM_PROVIDER, null, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
