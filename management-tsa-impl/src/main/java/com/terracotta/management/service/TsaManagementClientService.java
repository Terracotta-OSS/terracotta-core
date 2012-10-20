/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.tc.config.schema.L2Info;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing an abstraction of the TSA capabilities.
 *
 * @author Ludovic Orban
 */
public interface TsaManagementClientService {

  Collection<ThreadDumpEntity> clientsThreadDump() throws ServiceExecutionException;

  Collection<ThreadDumpEntity> serversThreadDump() throws ServiceExecutionException;

  ServerEntity buildServerEntity(L2Info l2Info) throws ServiceExecutionException;

  Collection<ClientEntity> buildClientEntities() throws ServiceExecutionException;

  TopologyEntity getTopology() throws ServiceExecutionException;

  StatisticsEntity getClientStatistics(String clientId) throws ServiceExecutionException;

  StatisticsEntity getServerStatistics(String serverName) throws ServiceExecutionException;

  Set<String> getAllClientIds() throws ServiceExecutionException;

  Set<String> getAllServerNames() throws ServiceExecutionException;

  boolean runDgc() throws ServiceExecutionException;

  Collection<StatisticsEntity> getDgcStatistics(int maxDgcStatsEntries) throws ServiceExecutionException;

}
