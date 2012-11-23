/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class TerracottaToolkitCreator {

  private static final String            TOOLKIT_IMPL_CLASSNAME            = "com.terracotta.toolkit.TerracottaToolkit";
  private static final String            ENTERPRISE_TOOLKIT_IMPL_CLASSNAME = "com.terracotta.toolkit.EnterpriseTerracottaToolkit";

  private static final String            NON_STOP_TOOLKIT_IMPL_CLASSNAME   = "com.terracotta.toolkit.NonStopToolkit";

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
      if (config.isNonStopEnabled()) {
        final FutureTask<ToolkitInternal> futureTask = createInternalToolkitAsynchronously();
        return instantiateNonStopToolkit(futureTask);
      } else {
        return createInternalToolkit();
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to create toolkit.", e);
    }
  }

  private FutureTask<ToolkitInternal> createInternalToolkitAsynchronously() {
    Callable<ToolkitInternal> callable = new Callable<ToolkitInternal>() {
      @Override
      public ToolkitInternal call() throws Exception {
        return createInternalToolkit();
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

  private ToolkitInternal createInternalToolkit() throws Exception {
    ToolkitInternal toolkitInternal = null;
    internalClient.init();

    if (enterprise) {
      toolkitInternal = instantiateToolkit(ENTERPRISE_TOOLKIT_IMPL_CLASSNAME);
    } else {
      toolkitInternal = instantiateToolkit(TOOLKIT_IMPL_CLASSNAME);
    }
    return toolkitInternal;
  }

  private TerracottaInternalClient createInternalClient() {
    try {
      return TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(config);
    } catch (Exception e) {
      throw new RuntimeException("Unable to create Terracotta L1 Client", e);
    }
  }

  private ToolkitInternal instantiateToolkit(String toolkitImplClassName) throws Exception {
    return internalClient.instantiate(toolkitImplClassName, new Class[] { TerracottaL1Instance.class },
                                      new Object[] { getTerracottaL1Instance() });
  }

  private ToolkitInternal instantiateNonStopToolkit(FutureTask<ToolkitInternal> futureTask) throws Exception {
    return internalClient.instantiate(NON_STOP_TOOLKIT_IMPL_CLASSNAME,
                                      new Class[] { FutureTask.class,
                                          internalClient.loadClass(PlatformService.class.getName()) }, new Object[] {
                                          futureTask, internalClient.getPlatformService() });
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

}
