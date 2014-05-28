/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.OperatorEventsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class OperatorEventsServiceImpl implements OperatorEventsService {

  private final ServerManagementService serverManagementService;

  public OperatorEventsServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, String sinceWhen, String eventLevels, boolean read) throws ServiceExecutionException {
    Set<String> acceptableLevels = null;
    if (eventLevels != null) {
      acceptableLevels = new HashSet<String>(Arrays.asList(eventLevels.split(",")));
    }

    if (sinceWhen == null) {
      return serverManagementService.getOperatorEvents(serverNames, null, acceptableLevels, read);
    } else {
      try {
        return serverManagementService.getOperatorEvents(serverNames, TimeStringParser.parseTime(sinceWhen), acceptableLevels, read);
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

  @Override
  public boolean markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException {
    return serverManagementService.markOperatorEvent(operatorEventEntity, read);
  }

  @Override
  public Map<String, Integer> getUnreadCount(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getUnreadOperatorEventCount(serverNames);
  }
}
