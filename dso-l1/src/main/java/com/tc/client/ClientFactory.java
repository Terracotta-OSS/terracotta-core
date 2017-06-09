/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.client;

import com.tc.lang.TCThreadGroup;
import com.tc.util.ProductID;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.ClientBuilder;
import com.tc.object.DistributedObjectClient;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tcclient.cluster.ClusterInternal;


public class ClientFactory {
  // Note that we don't currently use classProvider in this path but it is left here as a remnant from the old shape until
  //  we can verify that it won't be used here.
  public static DistributedObjectClient createClient(ClientConfig config, ClientBuilder builder, TCThreadGroup threadGroup,
                                                     PreparedComponentsFromL2Connection connectionComponents,
                                                     ClusterInternal cluster,
                                                     TCSecurityManager securityManager,
                                                     String uuid, String name) {
    return new DistributedObjectClient(config, builder, threadGroup, connectionComponents,
        cluster, null,
        uuid, name);
  }
}
