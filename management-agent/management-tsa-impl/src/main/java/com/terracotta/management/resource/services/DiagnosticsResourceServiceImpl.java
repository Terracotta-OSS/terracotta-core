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

import com.tc.license.ProductID;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.DiagnosticsService;

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

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.terracotta.management.resource.services.utils.AttachmentUtils.createTimestampedZipFilename;

/**
 * @author Ludovic Orban
 */
@Path("/agents/diagnostics")
public class DiagnosticsResourceServiceImpl implements DiagnosticsResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(DiagnosticsResourceServiceImpl.class);

  private final DiagnosticsService diagnosticsService;
  private final RequestValidator requestValidator;

  public DiagnosticsResourceServiceImpl() {
    this.diagnosticsService = ServiceLocator.locate(DiagnosticsService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  private InputStream zipAndConvertToInputStream(Collection<ThreadDumpEntity> threadDumpEntities) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream out = new ZipOutputStream(baos);

    for (ThreadDumpEntity threadDumpEntity : threadDumpEntities) {
      out.putNextEntry(new ZipEntry(threadDumpEntity.getSourceId().replace(':', '_') + ".txt"));
      out.write(threadDumpEntity.getDump().getBytes(Charset.forName("UTF-8")));
      out.closeEntry();
    }
    out.close();

    return new ByteArrayInputStream(baos.toByteArray());
  }

  @Override
  public Collection<ThreadDumpEntity> clusterThreadDump(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clusterThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<ProductID> productIDs = UriInfoUtils.extractProductIds(info);
      return diagnosticsService.getClusterThreadDump(productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Response clusterThreadDumpZipped(UriInfo info) {
    Collection<ThreadDumpEntity> threadDumpEntities = clusterThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("clusterThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ThreadDumpEntity> serversThreadDump(UriInfo info) {
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

  @Override
  public Response serversThreadDumpZipped(UriInfo info) {
    Collection<ThreadDumpEntity> threadDumpEntities = serversThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("serversThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clientsThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(3).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      Set<ProductID> productIDs = UriInfoUtils.extractProductIds(info);
      return diagnosticsService.getClientsThreadDump(clientIds, productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Response clientsThreadDumpZipped(UriInfo info) {
    Collection<ThreadDumpEntity> threadDumpEntities = clientsThreadDump(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(threadDumpEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("clientsThreadDump")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public boolean runDgc(UriInfo info) {
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

  @Override
  public boolean dumpClusterState(UriInfo info) {
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

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.reloadConfiguration: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.reloadConfiguration();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
