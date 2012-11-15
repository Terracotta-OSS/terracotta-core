/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceMBean;
import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ClientEntity;
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

  Collection<ClientEntity> getClientEntities() throws ServiceExecutionException;

  TopologyEntity getTopology() throws ServiceExecutionException;

  StatisticsEntity getClientStatistics(String clientId, Set<String> attributes) throws ServiceExecutionException;

  StatisticsEntity getServerStatistics(String serverName, Set<String> attributes) throws ServiceExecutionException;

  Set<String> getAllClientIds() throws ServiceExecutionException;

  Set<String> getAllServerNames() throws ServiceExecutionException;

  boolean runDgc() throws ServiceExecutionException;

  Collection<StatisticsEntity> getDgcStatistics(int maxDgcStatsEntries) throws ServiceExecutionException;

  Set<String> getL2Urls() throws ServiceExecutionException;

  Set<String> getL1Nodes() throws ServiceExecutionException;

  boolean isEnterpriseEdition() throws ServiceExecutionException;

  byte[] invokeMethod(String validatedNode, Class<DfltSamplerRepositoryServiceMBean> clazz, String ticket, String token,
                      String securityCallbackUrl, String methodName, Class<?>[] paramClasses, Object[] params) throws ServiceExecutionException;

}
