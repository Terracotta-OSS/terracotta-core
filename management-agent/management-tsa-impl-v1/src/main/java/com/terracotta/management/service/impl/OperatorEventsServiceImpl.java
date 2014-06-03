/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.OperatorEventsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class OperatorEventsServiceImpl implements OperatorEventsService {

  private final ServerManagementService serverManagementService;

  public OperatorEventsServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, String sinceWhen, String eventTypes, String eventLevels, boolean read) throws ServiceExecutionException {
    Set<String> acceptableLevels = null;
    if (eventLevels != null) {
      acceptableLevels = new HashSet<String>(Arrays.asList(eventLevels.split(",")));
    }
    
    Set<String> acceptableTypes = null;
    if (eventTypes != null) {
      acceptableTypes = new HashSet<String>(Arrays.asList(eventTypes.split(",")));
    }

    if (sinceWhen == null) {
      return serverManagementService.getOperatorEvents(serverNames, null, acceptableTypes, acceptableLevels, read);
    } else {
      try {
        return serverManagementService.getOperatorEvents(serverNames, TimeStringParser.parseTime(sinceWhen), acceptableTypes, acceptableLevels, read);
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

  @Override
  public boolean markOperatorEvents(Collection<OperatorEventEntity> operatorEventEntities, boolean read) throws ServiceExecutionException {
    
    boolean rc = true;
    for (OperatorEventEntity operatorEventEntity : operatorEventEntities) {
      try {
        if (operatorEventEntity.getEventLevel() == null) {
          throw new ServiceExecutionException("eventLevel must not be null");
        }
        if (operatorEventEntity.getEventSubsystem() == null) {
          throw new ServiceExecutionException("eventSubsystem must not be null");
        }
        if (operatorEventEntity.getEventType() == null) {
          throw new ServiceExecutionException("eventType must not be null");
        }
        if (operatorEventEntity.getCollapseString() == null) {
          throw new ServiceExecutionException("collapseString must not be null");
        }
        if (operatorEventEntity.getSourceId() == null) {
          throw new ServiceExecutionException("sourceId must not be null");
        }
        if (operatorEventEntity.getTimestamp() == 0L) {
          throw new ServiceExecutionException("timestamp must not be 0");
        }

        rc &= serverManagementService.markOperatorEvent(operatorEventEntity, read);
      } catch (ServiceExecutionException see) {
        throw new ResourceRuntimeException("Failed to mark TSA operator event as read", see, Response.Status.BAD_REQUEST.getStatusCode());
      }
    }
    
    return rc;

  }

  @Override
  public Map<String, Integer> getUnreadCount(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getUnreadOperatorEventCount(serverNames);
  }
}
