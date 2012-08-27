/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

public class TerracottaToolkitCreator {

  private static final String            TOOLKIT_IMPL_CLASSNAME            = "com.terracotta.toolkit.TerracottaToolkit";
  private static final String            ENTERPRISE_TOOLKIT_IMPL_CLASSNAME = "com.terracotta.toolkit.EnterpriseTerracottaToolkit";

  private final TerracottaInternalClient internalClient;
  private final boolean                  enterprise;

  public TerracottaToolkitCreator(TerracottaClientConfig config, boolean enterprise) {
    this.enterprise = enterprise;
    if (config == null) { throw new NullPointerException("terracottaClientConfig cannot be null"); }
    internalClient = createInternalClient(config);
  }

  public ToolkitInternal createToolkit() {
    try {
      if (enterprise) {
        return instantiateToolkit(ENTERPRISE_TOOLKIT_IMPL_CLASSNAME);
      } else {
        return instantiateToolkit(TOOLKIT_IMPL_CLASSNAME);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to create toolkit.", e);
    }
  }

  private TerracottaInternalClient createInternalClient(TerracottaClientConfig config) {
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
