/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA diagnostics.
 *
 * @author Ludovic Orban
 */
public interface DiagnosticsResourceService {

  @GET
  @Path("/threadDump")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ThreadDumpEntity> clusterThreadDump(@Context UriInfo info);

  @GET
  @Path("/threadDumpArchive")
  @Produces("application/zip")
  Response clusterThreadDumpZipped(@Context UriInfo info);

  @GET
  @Path("/threadDump/servers")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ThreadDumpEntity> serversThreadDump(@Context UriInfo info);

  @GET
  @Path("/threadDumpArchive/servers")
  @Produces("application/zip")
  Response serversThreadDumpZipped(@Context UriInfo info);

  @GET
  @Path("/threadDump/clients")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ThreadDumpEntity> clientsThreadDump(@Context UriInfo info);

  @GET
  @Path("/threadDumpArchive/clients")
  @Produces("application/zip")
  Response clientsThreadDumpZipped(@Context UriInfo info);

  @POST
  @Path("/dgc")
  @Produces(MediaType.APPLICATION_JSON)
  boolean runDgc(@Context UriInfo info);

  @POST
  @Path("/dumpClusterState")
  @Produces(MediaType.APPLICATION_JSON)
  boolean dumpClusterState(@Context UriInfo info);

  @POST
  @Path("/reloadConfiguration")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<TopologyReloadStatusEntity> reloadConfiguration(@Context UriInfo info);

}
