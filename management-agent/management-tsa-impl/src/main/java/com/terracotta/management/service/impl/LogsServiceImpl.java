/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.joda.time.DateTime;
import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.service.LogsService;
import com.terracotta.management.service.TsaManagementClientService;

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
      return getLogs(serverNames, parseTime(sinceWhen));
    }
  }

  private long parseTime(String timeString) throws ServiceExecutionException {
    try {
      if (timeString.endsWith("d")) {
        String days = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusDays(Integer.parseInt(days)).getMillis();
      } else if (timeString.endsWith("h")) {
        String hours = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusHours(Integer.parseInt(hours)).getMillis();
      } else if (timeString.endsWith("m")) {
        String minutes = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusMinutes(Integer.parseInt(minutes)).getMillis();
      } else if (timeString.endsWith("s")) {
        String seconds = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusSeconds(Integer.parseInt(seconds)).getMillis();
      } else {
        throw new ServiceExecutionException("Illegal time string: [" + timeString + "]");
      }
    } catch (NumberFormatException nfe) {
      throw new ServiceExecutionException("Illegal time string: [" + timeString + "]", nfe);
    }
  }

}
