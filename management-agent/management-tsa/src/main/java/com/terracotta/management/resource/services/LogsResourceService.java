/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.LogEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA logs.
 *
 * @author Ludovic Orban
 */
public interface LogsResourceService {

  public final static String ATTR_QUERY_KEY = "sinceWhen";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<LogEntity> getLogs(@Context UriInfo info);

  @GET
  @Path("/archive")
  @Produces("application/zip")
  Response getLogsZipped(@Context UriInfo info);

}
