/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.mock;

import com.tc.config.schema.StatisticsConfig;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemCallback;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.retrieval.NullStatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.util.UUID;

import javax.management.MBeanServer;

public class NullStatisticsAgentSubSystem implements StatisticsAgentSubSystem {

  public boolean isActive() {
    return false;
  }

  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry() {
    return NullStatisticsRetrievalRegistry.INSTANCE;
  }

  public AgentStatisticsManager getStatisticsManager() {
    return NullAgentStatisticsManager.INSTANCE;
  }

  public void cleanup() throws Exception {
    // no-op
  }

  public void registerMBeans(final MBeanServer server) {
    // nothing to register
  }

  public void registerMBeans(MBeanServer server, UUID id) {
    /**/
  }

  public void setDefaultAgentDifferentiator(final String defaultAgentDifferentiator) {
    // no-op
  }

  public void setDefaultAgentIp(final String defaultAgentIp) {
    // no-op
  }

  public boolean setup(final StatisticsSystemType type, final StatisticsConfig config) {
    // nothing to setup
    return true;
  }

  public void unregisterMBeans(final MBeanServer server) {
    // nothing to unregister
  }

  public void removeCallback(final StatisticsAgentSubSystemCallback callback) {
    // no-op
  }

  public void addCallback(final StatisticsAgentSubSystemCallback callback) {
    // no-op
  }

  public boolean waitUntilSetupComplete() {
    return false;
  }
}
