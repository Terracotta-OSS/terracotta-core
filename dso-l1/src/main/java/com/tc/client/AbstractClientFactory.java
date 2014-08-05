/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.abortable.AbortableOperationManager;
import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.loaders.ClassProvider;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.util.UUID;
import com.tc.util.factory.AbstractFactory;
import com.tcclient.cluster.DsoClusterInternal;

import java.util.Map;

public abstract class AbstractClientFactory extends AbstractFactory {
  private static String FACTORY_SERVICE_ID            = "com.tc.client.ClientFactory";
  private static Class  STANDARD_CLIENT_FACTORY_CLASS = StandardClientFactory.class;

  public static AbstractClientFactory getFactory() {
    return (AbstractClientFactory) getFactory(FACTORY_SERVICE_ID, STANDARD_CLIENT_FACTORY_CLASS);
  }

  public abstract DistributedObjectClient createClient(DSOClientConfigHelper config, TCThreadGroup threadGroup,
                                                       ClassProvider classProvider,
                                                       PreparedComponentsFromL2Connection connectionComponents,
                                                       DsoClusterInternal dsoCluster,
                                                       TCSecurityManager securityManager,
                                                       AbortableOperationManager abortableOperationManager,
                                                       RejoinManagerInternal rejoinManager, UUID uuid,
                                                       ProductID productId);

  public abstract TCSecurityManager createClientSecurityManager(Map<String, Object> env);
}
