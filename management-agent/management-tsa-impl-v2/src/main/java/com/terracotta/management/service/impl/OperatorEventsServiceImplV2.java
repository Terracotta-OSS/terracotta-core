/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.OperatorEventEntityV2;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.OperatorEventsServiceV2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class OperatorEventsServiceImplV2 implements OperatorEventsServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;

  public OperatorEventsServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<OperatorEventEntityV2> getOperatorEvents(Set<String> serverNames, String sinceWhen, String eventTypes, boolean read) throws ServiceExecutionException {
    Set<String> acceptableTypes = null;
    if (eventTypes != null) {
      acceptableTypes = new HashSet<String>(Arrays.asList(eventTypes.split(",")));
    }

    if (sinceWhen == null) {
      return serverManagementService.getOperatorEvents(serverNames, null, acceptableTypes, read);
    } else {
      try {
        return serverManagementService.getOperatorEvents(serverNames, TimeStringParser.parseTime(sinceWhen), acceptableTypes, read);
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

  @Override
  public boolean markOperatorEvent(OperatorEventEntityV2 operatorEventEntityV2, boolean read) throws ServiceExecutionException {
    return serverManagementService.markOperatorEvent(operatorEventEntityV2, read);
  }

  @Override
  public Map<String, Integer> getUnreadCount(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getUnreadOperatorEventCount(serverNames);
  }
}
