/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.lang.TCThreadGroup;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.RuntimeLogger;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.util.factory.AbstractFactory;
import com.tcclient.cluster.DsoClusterInternal;

public abstract class AbstractClientFactory extends AbstractFactory {
  private static String FACTORY_SERVICE_ID            = "com.tc.client.ClientFactory";
  private static Class  STANDARD_CLIENT_FACTORY_CLASS = StandardClientFactory.class;

  public static AbstractClientFactory getFactory() {
    return (AbstractClientFactory) getFactory(FACTORY_SERVICE_ID, STANDARD_CLIENT_FACTORY_CLASS);
  }

  public abstract DistributedObjectClient createClient(DSOClientConfigHelper config, TCThreadGroup threadGroup,
                                                       ClassProvider classProvider,
                                                       PreparedComponentsFromL2Connection connectionComponents,
                                                       Manager manager,
                                                       StatisticsAgentSubSystem statisticsAgentSubSystem,
                                                       DsoClusterInternal dsoCluster, RuntimeLogger runtimeLogger,
                                                       ClientMode clientMode);
}
