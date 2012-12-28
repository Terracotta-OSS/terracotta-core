/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.service.ShutdownService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ShutdownServiceImpl implements ShutdownService {

  private final TsaManagementClientService tsaManagementClientService;

  public ShutdownServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public void shutdown(Set<String> serverNames) throws ServiceExecutionException {
    tsaManagementClientService.shutdownServers(serverNames);
  }

}
