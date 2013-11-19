/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.tc.license.ProductID;
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
   * Get the statistics of the specified client.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<ProductID> clientProductIds) throws ServiceExecutionException;

  /**
   * Get the statistics of the specified server.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException;

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
