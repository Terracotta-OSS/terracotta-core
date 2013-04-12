/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.OperatorEventEntity;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA operator events.
 *
 * @author Ludovic Orban
 */
public interface OperatorEventsResourceService {

  public final static String ATTR_QUERY_KEY__SINCE_WHEN = "sinceWhen";
  public final static String ATTR_QUERY_KEY__EVENT_TYPES = "eventTypes";
  public final static String ATTR_FILTER_KEY = "filterOutRead";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<OperatorEventEntity> getOperatorEvents(@Context UriInfo info);

  @POST
  @Path("/read")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  boolean markOperatorEventAsRead(@Context UriInfo info, Collection<OperatorEventEntity> operatorEventEntities);

  @POST
  @Path("/unread")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  boolean markOperatorEventAsUnread(@Context UriInfo info, Collection<OperatorEventEntity> operatorEventEntities);

}
