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

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public interface TsaManagementClientService {

  Collection<ThreadDumpEntity> clientsThreadDump() throws ServiceExecutionException;

  Collection<ThreadDumpEntity> serversThreadDump() throws ServiceExecutionException;

  ServerEntity buildServerEntity(L2Info l2Info) throws ServiceExecutionException;

  Collection<ClientEntity> buildClientEntities() throws ServiceExecutionException;

  StatisticsEntity getClientStatistics(String clientId) throws ServiceExecutionException;

  Set<String> getAllClientIds() throws ServiceExecutionException;

  boolean runDgc() throws ServiceExecutionException;
}
