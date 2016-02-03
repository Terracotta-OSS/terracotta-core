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

package com.tc.server;

import com.tc.config.HaConfigImpl;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;

public class ServerFactory {
  private final static int MAX_CLIENTS = Integer.MAX_VALUE;
  
  public static TCServer createServer(L2ConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    // only coordinator checks license
    boolean isCoordinatorGroup = new HaConfigImpl(configurationSetupManager).isActiveCoordinatorGroup();
    ConnectionPolicy policy = isCoordinatorGroup ? new ConnectionPolicyImpl(MAX_CLIENTS)
        : new NullConnectionPolicy();
    return new TCServerImpl(configurationSetupManager, threadGroup, policy);
  }
}
