/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

/**
 * This interface provides high-level access to the CVT sub-system that is
 * used for each gatherer.
 */
public interface StatisticsAgentSubSystem {
  /**
   * Indicates whether the sub-system is active.
   *
   * @return {@code true} when the sub-system is active; or
   * {@code false} otherwise
   */
  public boolean isActive();

  /**
   * Returns the {@link StatisticsRetrievalRegistry} that is used by this
   * agent sub-system.
   *
   * @return the requested retrieval registry
   */
  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry();

  /**
   * Returns the {@link AgentStatisticsManager} that is used by this agent
   * sub-system.
   *
   * @return the requested manager
   */
  public AgentStatisticsManager getStatisticsManager();

  /**
   * Cleans up the resources allocated by this agent.
   */
  public void cleanup() throws Exception;
}