/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.exceptions.AgentStatisticsManagerException;

import java.util.Collection;

/**
 * This interface provides methods that are specific to the
 * {@code StatisticsManager} that is used by the {@link StatisticsAgentSubSystem},
 * they are not part of the general {@code StatisticsManager} interface.
 */
public interface AgentStatisticsManager {
  /**
   * Retrieves all the session IDs that are active and have a particular
   * action registered.
   *
   * @param actionName the name of the action that has to be searched for
   * @return the requested collection of session IDs as strings; or
   * an empty collection of no session ID could be found
   */
  public Collection getActiveSessionIDsForAction(String actionName);

  /**
   * Inject new statistics data into the agent for a particular session ID.
   *
   * The session ID will be automatically filled into the data instance.
   *
   * This is particularly useful for {@link StatisticRetrievalAction} classes
   * that are not captured automatically, but rather on a triggered basis, for
   * instance when a particular event or condition is met in the system.
   *
   * @param sessionId the session ID that this data has to be added for
   * @param data the data that will be added
   * @throws com.tc.statistics.exceptions.AgentStatisticsManagerException in case the session ID couldn't
   * be found amongst the active session; or
   * if the data instance doesn't contain mandatory properties
   */
  public void injectStatisticData(String sessionId, StatisticData data) throws AgentStatisticsManagerException;
}
