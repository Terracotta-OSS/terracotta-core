/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics;

import com.tc.config.schema.StatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.util.UUID;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

/**
 * This interface provides high-level access to the statistics sub-system that is used for each gatherer.
 */
public interface StatisticsAgentSubSystem {
  /**
   * Indicates whether the sub-system is active.
   * 
   * @return {@code true} when the sub-system is active; or {@code false} otherwise
   */
  public boolean isActive();

  /**
   * Returns the {@link StatisticsRetrievalRegistry} that is used by this agent sub-system.
   * 
   * @return the requested retrieval registry
   */
  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry();

  /**
   * Returns the {@link AgentStatisticsManager} that is used by this agent sub-system.
   * 
   * @return the requested manager
   */
  public AgentStatisticsManager getStatisticsManager();

  /**
   * Cleans up the resources allocated by this agent.
   */
  public void cleanup() throws Exception;

  /**
   * Sets up the system based on the Terracotta configuration
   * 
   * @return {@code true} if the statistics agent was setup successfully; or {@code false} otherwise
   */
  public boolean setup(StatisticsSystemType type, StatisticsConfig config);

  public boolean waitUntilSetupComplete();

  public void setDefaultAgentIp(String defaultAgentIp);

  public void setDefaultAgentDifferentiator(String defaultAgentDifferentiator);

  public void registerMBeans(MBeanServer server) throws MBeanRegistrationException, NotCompliantMBeanException,
      InstanceAlreadyExistsException;

  void registerMBeans(final MBeanServer server, UUID uuid) throws MBeanRegistrationException,
      NotCompliantMBeanException, InstanceAlreadyExistsException, MalformedObjectNameException;

  public void unregisterMBeans(MBeanServer server) throws MBeanRegistrationException;

  public void addCallback(final StatisticsAgentSubSystemCallback callback);

  public void removeCallback(final StatisticsAgentSubSystemCallback callback);
}