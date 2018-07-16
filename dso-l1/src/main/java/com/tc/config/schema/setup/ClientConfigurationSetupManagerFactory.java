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

package com.tc.config.schema.setup;

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author vmad
 */
public class ClientConfigurationSetupManagerFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfigurationSetupManagerFactory.class);

  private final String[] args;
  private final List<InetSocketAddress> stripeMemberUris;

  public ClientConfigurationSetupManagerFactory(String[] args, List<InetSocketAddress> stripeMemberUris) {
    this.args = args;
    this.stripeMemberUris = stripeMemberUris;
  }

  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager() throws ConfigurationSetupException {
    return new ClientConfigurationSetupManager(this.stripeMemberUris, args);
  }
}
