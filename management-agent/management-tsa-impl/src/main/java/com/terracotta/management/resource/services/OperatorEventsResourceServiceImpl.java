/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.OperatorEventsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/operatorEvents")
public class OperatorEventsResourceServiceImpl implements OperatorEventsResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(OperatorEventsResourceServiceImpl.class);

  private final OperatorEventsService operatorEventsService;
  private final RequestValidator requestValidator;

  public OperatorEventsResourceServiceImpl() {
    this.operatorEventsService = ServiceLocator.locate(OperatorEventsService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(UriInfo info) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImpl.getOperatorEvents: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String sinceWhen = qParams.getFirst(ATTR_QUERY_KEY__SINCE_WHEN);
      String eventTypes = qParams.getFirst(ATTR_QUERY_KEY__EVENT_TYPES);
      boolean filterOutRead = qParams.getFirst(ATTR_FILTER_KEY) == null || Boolean.parseBoolean(qParams.getFirst(ATTR_FILTER_KEY));

      return operatorEventsService.getOperatorEvents(serverNames, sinceWhen, eventTypes, filterOutRead);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA operator events", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public boolean markOperatorEventAsRead(UriInfo info, Collection<OperatorEventEntity> operatorEventEntities) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImpl.markOperatorEventAsRead: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    boolean rc = true;
    for (OperatorEventEntity operatorEventEntity : operatorEventEntities) {
      try {
        if (operatorEventEntity.getEventType() == null) {
          throw new ServiceExecutionException("eventType must not be null");
        }
        if (operatorEventEntity.getEventSubsystem() == null) {
          throw new ServiceExecutionException("eventSubsystem must not be null");
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

        rc &= operatorEventsService.markOperatorEvent(operatorEventEntity, true);
      } catch (ServiceExecutionException see) {
        throw new ResourceRuntimeException("Failed to mark TSA operator event as read", see, Response.Status.BAD_REQUEST.getStatusCode());
      }
    }

    return rc;
  }

  @Override
  public boolean markOperatorEventAsUnread(UriInfo info, Collection<OperatorEventEntity> operatorEventEntities) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImpl.markOperatorEventAsUnread: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    boolean rc = true;
    for (OperatorEventEntity operatorEventEntity : operatorEventEntities) {
      try {
        if (operatorEventEntity.getEventType() == null) {
          throw new ServiceExecutionException("eventType must not be null");
        }
        if (operatorEventEntity.getEventSubsystem() == null) {
          throw new ServiceExecutionException("eventSubsystem must not be null");
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

        rc &= operatorEventsService.markOperatorEvent(operatorEventEntity, false);
      } catch (ServiceExecutionException see) {
        throw new ResourceRuntimeException("Failed to mark TSA operator event as unread", see, Response.Status.BAD_REQUEST.getStatusCode());
      }
    }

    return rc;
  }

}
