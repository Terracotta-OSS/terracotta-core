/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.service.ShutdownServiceV2;

import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ShutdownServiceImplV2 implements ShutdownServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;

  public ShutdownServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public void shutdown(Set<String> serverNames) throws ServiceExecutionException {
    serverManagementService.shutdownServers(serverNames);
  }

}
