/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An interface for service implementations providing an abstraction of the TSA capabilities.
 *
 * @author Ludovic Orban
 */
public interface TsaManagementClientService extends RemoteAgentBridgeService {

  Collection<ThreadDumpEntity> clusterThreadDump() throws ServiceExecutionException;

  Collection<ThreadDumpEntity> clientsThreadDump(Set<String> clientIds) throws ServiceExecutionException;

  Collection<ThreadDumpEntity> serversThreadDump(Set<String> serverNames) throws ServiceExecutionException;

  Collection<ClientEntity> getClientEntities() throws ServiceExecutionException;

  Collection<ServerGroupEntity> getTopology() throws ServiceExecutionException;

  Collection<StatisticsEntity> getClientsStatistics(Set<String> clientIds, Set<String> attributes) throws ServiceExecutionException;

  Collection<StatisticsEntity> getServersStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException;

  boolean runDgc(Set<String> serverNames) throws ServiceExecutionException;

  boolean dumpClusterState(Set<String> serverNames) throws ServiceExecutionException;

  Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames, int maxDgcStatsEntries) throws ServiceExecutionException;

  Collection<String> getL2Urls() throws ServiceExecutionException;

  boolean isEnterpriseEdition() throws ServiceExecutionException;

  Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException;

  Collection<ConfigEntity> getClientConfigs(Set<String> clientIds) throws ServiceExecutionException;

  Collection<BackupEntity> getBackupsStatus() throws ServiceExecutionException;

  Collection<BackupEntity> backup() throws ServiceExecutionException;

  Collection<LogEntity> getLogs(Set<String> serverNames, Long sinceWhen) throws ServiceExecutionException;

  Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, Long sinceWhen, Set<String> acceptableTypes, boolean read) throws ServiceExecutionException;

  boolean markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException;

  void shutdownServers(Set<String> serverNames) throws ServiceExecutionException;

  Collection<TopologyReloadStatusEntity> reloadConfiguration() throws ServiceExecutionException;

  Map<String, Integer> getUnreadOperatorEventCount() throws ServiceExecutionException;

  List<String> performSecurityChecks();

  Collection<MBeanEntity> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException;
}
