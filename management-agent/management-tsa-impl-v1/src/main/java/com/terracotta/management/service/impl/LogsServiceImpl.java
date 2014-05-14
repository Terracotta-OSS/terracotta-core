/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.LogsService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class LogsServiceImpl implements LogsService {

  private final ServerManagementService serverManagementService;

  public LogsServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getLogs(serverNames, null);
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, long sinceWhen) throws ServiceExecutionException {
    return serverManagementService.getLogs(serverNames, sinceWhen);
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, String sinceWhen) throws ServiceExecutionException {
    if (sinceWhen == null) {
      return getLogs(serverNames);
    } else {
      try {
        return getLogs(serverNames, TimeStringParser.parseTime(sinceWhen));
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

}
