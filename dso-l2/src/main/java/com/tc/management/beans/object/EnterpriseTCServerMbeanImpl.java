/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
