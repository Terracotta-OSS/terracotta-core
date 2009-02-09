/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.cluster.Cluster;
import com.tc.cluster.DsoClusterInternal;
import com.tc.lang.TCThreadGroup;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.RuntimeLogger;

public class StandardClientFactory extends AbstractClientFactory {

  @Override
  public DistributedObjectClient createClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                              final ClassProvider classProvider,
                                              final PreparedComponentsFromL2Connection connectionComponents,
                                              final Manager manager, final Cluster cluster,
                                              final DsoClusterInternal dsoCluster, final RuntimeLogger runtimeLogger) {
    return new DistributedObjectClient(config, threadGroup, classProvider, connectionComponents, manager, cluster,
                                       dsoCluster, runtimeLogger);
  }
}
