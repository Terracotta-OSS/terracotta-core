/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.EnterpriseDistributedObjectClient;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.util.UUID;
import com.tcclient.cluster.ClusterInternal;


public class ClientFactory {
  // Note that we don't currently use classProvider in this path but it is left here as a remnant from the old shape until
  //  we can verify that it won't be used here.
  public static DistributedObjectClient createClient(ClientConfig config, TCThreadGroup threadGroup,
                                                     PreparedComponentsFromL2Connection connectionComponents,
                                                     ClusterInternal cluster,
                                                     TCSecurityManager securityManager,
                                                     UUID uuid, ProductID productId) {
    //TODO add license check
    if(securityManager == null) {
      return new DistributedObjectClient(config, threadGroup, connectionComponents,
          cluster, null,
          uuid, productId);
    } else {
      return new EnterpriseDistributedObjectClient(config, threadGroup, connectionComponents, cluster, securityManager, uuid, productId);
    }
  }
}
