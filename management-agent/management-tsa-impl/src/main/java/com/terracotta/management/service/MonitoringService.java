/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.StatisticsEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA statistics monitoring facilities.
 *
 * @author Ludovic Orban
 */
public interface MonitoringService {

  /**
   * Get a set of all known client IDs currently connected.
   *
   * @return a set of all known client IDs currently connected.
   * @throws ServiceExecutionException
   */
  Set<String> getAllClientIds() throws ServiceExecutionException;

  /**
   * Get a set of all known server names currently connected.
   *
   * @return a set of all known server names currently connected.
   * @throws ServiceExecutionException
   */
  Set<String> getAllServerNames() throws ServiceExecutionException;

  /**
   * Get the statistics of the specified client.
   *
   * @param clientId the client ID.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a {@link StatisticsEntity} object representing the client statistics.
   * @throws ServiceExecutionException
   */
  StatisticsEntity getClientStatistics(String clientId, Set<String> attributes) throws ServiceExecutionException;

  /**
   * Get the statistics of the specified server.
   *
   * @param serverName the server name.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a {@link StatisticsEntity} object representing the server statistics.
   * @throws ServiceExecutionException
   */
  StatisticsEntity getServerStatistics(String serverName, Set<String> attributes) throws ServiceExecutionException;

  /**
   * Get the DGC statistics.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a {@link Collection} object of {@link StatisticsEntity} objects representing the DGC statistics,
   * one {@link StatisticsEntity} per DGC iteration.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException;

}
