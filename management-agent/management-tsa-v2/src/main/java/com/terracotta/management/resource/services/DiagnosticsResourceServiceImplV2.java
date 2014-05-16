/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import static com.terracotta.management.resource.services.utils.AttachmentUtils.createTimestampedZipFilename;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyReloadStatusEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidatorV2;
import com.terracotta.management.service.DiagnosticsServiceV2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
@Path("/v2/agents/diagnostics")
public class DiagnosticsResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(DiagnosticsResourceServiceImplV2.class);

  private final DiagnosticsServiceV2 diagnosticsService;
  private final RequestValidator requestValidator;

  public DiagnosticsResourceServiceImplV2() {
    this.diagnosticsService = ServiceLocator.locate(DiagnosticsServiceV2.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidatorV2.class);
  }

  private InputStream zipAndConvertToInputStream(Collection<ThreadDumpEntityV2> threadDumpEntities) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream out = new ZipOutputStream(baos);

    for (ThreadDumpEntityV2 threadDumpEntityV2 : threadDumpEntities) {
      out.putNextEntry(new ZipEntry(threadDumpEntityV2.getSourceId().replace(':', '_') + ".txt"));
      out.write(threadDumpEntityV2.getDump().getBytes(Charset.forName("UTF-8")));
      out.closeEntry();
    }
    out.close();

    return new ByteArrayInputStream(baos.toByteArray());
  }

  @GET
  @Path("/v2/threadDump")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ThreadDumpEntityV2> clusterThreadDump(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clusterThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      return diagnosticsService.getClusterThreadDump(productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/threadDumpArchive")
  @Produces("application/zip")
  public Response clusterThreadDumpZipped(@Context UriInfo info) {
    Collection<ThreadDumpEntityV2> threadDumpEntities = clusterThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("clusterThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/threadDump/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ThreadDumpEntityV2> serversThreadDump(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.serversThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return diagnosticsService.getServersThreadDump(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/threadDumpArchive/servers")
  @Produces("application/zip")
  public Response serversThreadDumpZipped(@Context UriInfo info) {
    Collection<ThreadDumpEntityV2> threadDumpEntities = serversThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("serversThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/threadDump/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ThreadDumpEntityV2> clientsThreadDump(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clientsThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(3).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      return diagnosticsService.getClientsThreadDump(clientIds, productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/threadDumpArchive/clients")
  @Produces("application/zip")
  public Response clientsThreadDumpZipped(@Context UriInfo info) {
    Collection<ThreadDumpEntityV2> threadDumpEntities = clientsThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("clientsThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Path("/v2/dgc")
  @Produces(MediaType.APPLICATION_JSON)
  public boolean runDgc(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.runDgc: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return diagnosticsService.runDgc(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Path("/v2/dumpClusterState")
  @Produces(MediaType.APPLICATION_JSON)
  public boolean dumpClusterState(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.dumpClusterState: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return diagnosticsService.dumpClusterState(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Path("/v2/reloadConfiguration")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<TopologyReloadStatusEntityV2> reloadConfiguration(@Context UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.reloadConfiguration: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return diagnosticsService.reloadConfiguration(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
