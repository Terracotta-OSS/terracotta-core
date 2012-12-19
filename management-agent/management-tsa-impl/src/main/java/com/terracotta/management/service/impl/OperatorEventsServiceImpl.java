/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.service.OperatorEventsService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.util.TimeStringParser;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class OperatorEventsServiceImpl implements OperatorEventsService {

  private final TsaManagementClientService tsaManagementClientService;

  public OperatorEventsServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, String sinceWhen, boolean read) throws ServiceExecutionException {
    if (sinceWhen == null) {
      return tsaManagementClientService.getOperatorEvents(serverNames, null, read);
    } else {
      try {
        return tsaManagementClientService.getOperatorEvents(serverNames, TimeStringParser.parseTime(sinceWhen), read);
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

  @Override
  public boolean markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException {
    return tsaManagementClientService.markOperatorEvent(operatorEventEntity, read);
  }
}
