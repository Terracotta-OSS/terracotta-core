/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.service.LogsService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.util.TimeStringParser;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class LogsServiceImpl implements LogsService {

  private final TsaManagementClientService tsaManagementClientService;

  public LogsServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.getLogs(serverNames, null);
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, long sinceWhen) throws ServiceExecutionException {
    return tsaManagementClientService.getLogs(serverNames, sinceWhen);
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
