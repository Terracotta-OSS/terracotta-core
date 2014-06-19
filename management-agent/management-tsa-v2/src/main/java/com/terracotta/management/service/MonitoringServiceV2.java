/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.StatisticsEntityV2;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA statistics monitoring facilities.
 *
 * @author Ludovic Orban
 */
public interface MonitoringServiceV2 {

  /**
   * Get the statistics of the specified client.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<StatisticsEntityV2> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<String> clientProductIds) throws ServiceExecutionException;

  /**
   * Get the statistics of the specified server.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<StatisticsEntityV2> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException;

  /**
   * Get the DGC statistics.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a {@link Collection} object of {@link StatisticsEntityV2} objects representing the DGC statistics,
   * one {@link StatisticsEntityV2} per DGC iteration.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<StatisticsEntityV2> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException;

}
