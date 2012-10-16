/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.ThreadDumpEntity;

import java.util.Collection;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA diagnostics.
 *
 * @author Ludovic Orban
 */
public interface DiagnosticsResourceService {

  @POST
  @Path("/threadDump")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ThreadDumpEntity> clusterThreadDump(@Context UriInfo info);

  @POST
  @Path("/dgc")
  @Produces(MediaType.APPLICATION_JSON)
  boolean runDgc(@Context UriInfo info);

}
