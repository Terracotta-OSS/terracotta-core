/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.service.ShutdownService;

import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ShutdownServiceImpl implements ShutdownService {

  private final ServerManagementService serverManagementService;

  public ShutdownServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public void shutdown(Set<String> serverNames) throws ServiceExecutionException {
    serverManagementService.shutdownServers(serverNames);
  }

}
