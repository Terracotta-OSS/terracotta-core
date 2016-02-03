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
package com.tc.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.util.ProductID;
import com.tc.net.core.ClusterTopologyChangedListener;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.config.ClientConfig;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.util.UUID;
import com.tcclient.cluster.ClusterInternal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the main point of entry into the active-active client.
 */
public class EnterpriseDistributedObjectClient extends DistributedObjectClient {

  private final List<ClusterTopologyChangedListener> listeners = Collections
      .synchronizedList(new ArrayList<ClusterTopologyChangedListener>());

  public EnterpriseDistributedObjectClient(ClientConfig config, TCThreadGroup threadGroup,
                                           PreparedComponentsFromL2Connection connectionComponents, ClusterInternal cluster,
                                           TCSecurityManager securityManager,
                                           UUID uuid, ProductID productId) {
    super(config, threadGroup, connectionComponents, cluster, securityManager, uuid, productId);
  }

  @Override
  protected ClientBuilder createClientBuilder() {
    return new EnterpriseNonAAClientBuilder();
  }

  @Override
  public void reloadConfiguration() throws ConfigurationSetupException {
    L1ConfigurationSetupManager newConfig = getClientConfigHelper().reloadServersConfiguration();

    PreparedComponentsFromL2Connection connComp = new PreparedComponentsFromL2Connection(newConfig);
    ConnectionInfoConfig[] connectionInfoItems = connComp.createConnectionInfoConfigItemByGroup();
    ConnectionAddressProvider[] addrProviders = new ConnectionAddressProvider[connectionInfoItems.length];
    for (int i = 0; i < connectionInfoItems.length; ++i) {
      ConnectionInfo[] connectionInfo = connectionInfoItems[i].getConnectionInfos();
      addrProviders[i] = new ConnectionAddressProvider(connectionInfo);
    }

    for (ClusterTopologyChangedListener listener : listeners) {
      listener.serversUpdated(addrProviders);
    }
  }

  @Override
  public void addServerConfigurationChangedListeners(ClusterTopologyChangedListener listener) {
    listeners.add(listener);
  }
}
