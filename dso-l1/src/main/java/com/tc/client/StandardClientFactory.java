/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.abortable.AbortableOperationManager;
import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.util.UUID;
import com.tcclient.cluster.DsoClusterInternal;

import java.util.Map;

public class StandardClientFactory extends AbstractClientFactory {

  @Override
  public DistributedObjectClient createClient(final DSOClientConfigHelper config, final TCThreadGroup threadGroup,
                                              final ClassProvider classProvider,
                                              final PreparedComponentsFromL2Connection connectionComponents,
                                              final Manager manager, final DsoClusterInternal dsoCluster,
                                              final TCSecurityManager securityManager,
                                              final AbortableOperationManager abortableOperationManager,
                                              final RejoinManagerInternal rejoinManager, UUID uuid, final ProductID productId) {
    return new DistributedObjectClient(config, threadGroup, classProvider, connectionComponents, manager, dsoCluster,
                                       securityManager, abortableOperationManager, rejoinManager, uuid, productId);
  }

  @Override
  public TCSecurityManager createClientSecurityManager(Map<String, Object> env) {
    return null;
  }
}
