/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.OperatorEventEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.OperatorEventsServiceV2;

import java.util.Collection;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA operator events.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/operatorEvents")
public class OperatorEventsResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(OperatorEventsResourceServiceImplV2.class);

  private final OperatorEventsServiceV2 operatorEventsService;
  private final RequestValidator requestValidator;

  public OperatorEventsResourceServiceImplV2() {
    this.operatorEventsService = ServiceLocator.locate(OperatorEventsServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_QUERY_KEY__SINCE_WHEN  = "sinceWhen";
  public final static String ATTR_QUERY_KEY__EVENT_TYPES = "eventTypes";
  public final static String ATTR_FILTER_KEY             = "filterOutRead";
  public final static String ATTR_QUERY_KEY__EVENT_LEVELS = "eventLevels";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<OperatorEventEntityV2> getOperatorEvents(@Context UriInfo info) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImplV2.getOperatorEvents: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String sinceWhen = qParams.getFirst(ATTR_QUERY_KEY__SINCE_WHEN);
      String eventTypes = qParams.getFirst(ATTR_QUERY_KEY__EVENT_TYPES);
      String eventLevels = qParams.getFirst(ATTR_QUERY_KEY__EVENT_LEVELS);
      boolean filterOutRead = qParams.getFirst(ATTR_FILTER_KEY) == null || Boolean.parseBoolean(qParams.getFirst(ATTR_FILTER_KEY));

      return operatorEventsService.getOperatorEvents(serverNames, sinceWhen, eventTypes, eventLevels, filterOutRead);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA operator events", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Path("/read")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public boolean markOperatorEventAsRead(@Context UriInfo info, Collection<OperatorEventEntityV2> operatorEventEntities) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImplV2.markOperatorEventAsRead: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return operatorEventsService.markOperatorEvents(operatorEventEntities, true);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to mark TSA operator event as read", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Path("/unread")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public boolean markOperatorEventAsUnread(@Context UriInfo info, Collection<OperatorEventEntityV2> operatorEventEntities) {
    LOG.debug(String.format("Invoking OperatorEventsResourceServiceImplV2.markOperatorEventAsUnread: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return operatorEventsService.markOperatorEvents(operatorEventEntities, false);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to mark TSA operator event as unread", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
