/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.tc.config.schema.L2Info;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ThreadDumpEntity;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public interface JmxClientService {

  Collection<ThreadDumpEntity> clientsThreadDump() throws ServiceExecutionException;

  Collection<ThreadDumpEntity> serversThreadDump() throws ServiceExecutionException;

  ServerEntity buildServerEntity(L2Info l2Info) throws ServiceExecutionException;

  Collection<ClientEntity> buildClientEntities() throws ServiceExecutionException;

}
