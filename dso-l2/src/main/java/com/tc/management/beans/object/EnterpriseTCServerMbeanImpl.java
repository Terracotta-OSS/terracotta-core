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
package com.tc.management.beans.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.server.ServerConnectionValidator;
import com.tc.server.TCServer;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class EnterpriseTCServerMbeanImpl extends AbstractNotifyingMBean implements EnterpriseTCServerMbean {
  private final TCServer                             server;
  private final L2ConfigurationSetupManager          l2ConfigurationSetupManager;
  private final ServerConnectionValidator            serverConnectionValidator;

  private static TCLogger                            logger        = TCLogging.getLogger(EnterpriseTCServerMbean.class);
  private static final TerracottaOperatorEventLogger opEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public EnterpriseTCServerMbeanImpl(TCServer server, L2ConfigurationSetupManager l2ConfigurationSetupManager,
                                     ServerConnectionValidator serverConnectionValidator)
      throws NotCompliantMBeanException {
    super(EnterpriseTCServerMbean.class);
    this.server = server;
    this.l2ConfigurationSetupManager = l2ConfigurationSetupManager;
    this.serverConnectionValidator = serverConnectionValidator;
  }

  @Override
  public synchronized TopologyReloadStatus reloadConfiguration() throws ConfigurationSetupException {
    // passing the operator event logger here because TerracottoOperatorEventLogger is singleton and
    // it is not supposed to be instantiated before the server starts and node name provide is
    // set in DistributedObjectServer.start
    // Since config is read before the server is started TerracottaOperatorEventLogger can not be
    // instantiated in L2ConfigurationSetupManagerImpl
    TopologyReloadStatus status = l2ConfigurationSetupManager.reloadConfiguration(serverConnectionValidator,
                                                                                  opEventLogger);
    if (status != TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE) {
      logger.warn("Reloading of configuration failed with the status " + status);
      return status;
    }

    server.reloadConfiguration();
    return status;
  }

  @Override
  public void reset() {
    //
  }
}
