/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.LogEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.LogsServiceV2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.terracotta.management.resource.services.utils.AttachmentUtils.createTimestampedZipFilename;

/**
 * A resource service for querying TSA logs.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/logs")
public class LogsResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(LogsResourceServiceImplV2.class);

  private final LogsServiceV2 logsService;
  private final RequestValidator requestValidator;

  public LogsResourceServiceImplV2() {
    this.logsService = ServiceLocator.locate(LogsServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_QUERY_KEY = "sinceWhen";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<LogEntityV2> getLogs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking LogsResourceServiceImplV2.getLogs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String sinceWhen = qParams.getFirst(ATTR_QUERY_KEY);

      return logsService.getLogs(serverNames, sinceWhen);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA logs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/archive")
  @Produces("application/zip")
  public Response getLogsZipped(@Context UriInfo info) {
    Collection<LogEntityV2> logEntities = getLogs(info).getEntities();

    try {
      InputStream inputStream = zipAndConvertToInputStream(logEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("logs")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to get TSA logs", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  private InputStream zipAndConvertToInputStream(Collection<LogEntityV2> unsortedLogEntities) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream out = new ZipOutputStream(baos);

    // Sort the logs per source then per timestamp.
    // Once that is done, the sorted logs can be processed sequentially by creating one zip entry each time a
    // new source is encountered and closing the previous one.
    List<LogEntityV2> sortedLogEntities = new ArrayList<LogEntityV2>(unsortedLogEntities);
    Collections.sort(sortedLogEntities, new Comparator<LogEntityV2>() {
      @Override
      public int compare(LogEntityV2 e1, LogEntityV2 e2) {
        int sourceComparison = e1.getSourceId().compareTo(e2.getSourceId());
        if (sourceComparison != 0) {
          return sourceComparison;
        }
        return (int)(e1.getTimestamp() - e2.getTimestamp());
      }
    });

    String lastSourceId = null;
    for (LogEntityV2 logEntityV2 : sortedLogEntities) {
      if (!logEntityV2.getSourceId().equals(lastSourceId)) {
        if (lastSourceId != null) {
          out.closeEntry();
        }
        out.putNextEntry(new ZipEntry(logEntityV2.getSourceId().replace(':', '_') + ".txt"));
        lastSourceId = logEntityV2.getSourceId();
      }
      out.write(logEntityV2.getMessage().getBytes(Charset.forName("UTF-8")));
    }
    out.closeEntry();
    out.close();

    return new ByteArrayInputStream(baos.toByteArray());
  }

}
