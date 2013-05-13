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

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.LogsService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static com.terracotta.management.resource.services.utils.AttachmentUtils.createTimestampedZipFilename;

/**
 * @author Ludovic Orban
 */
@Path("/agents/logs")
public class LogsResourceServiceImpl implements LogsResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(LogsResourceServiceImpl.class);

  private final LogsService logsService;
  private final RequestValidator requestValidator;

  public LogsResourceServiceImpl() {
    this.logsService = ServiceLocator.locate(LogsService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<LogEntity> getLogs(UriInfo info) {
    LOG.debug(String.format("Invoking LogsResourceServiceImpl.getLogs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String sinceWhen = qParams.getFirst(ATTR_QUERY_KEY);

      return logsService.getLogs(serverNames, sinceWhen);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA logs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Response getLogsZipped(UriInfo info) {
    Collection<LogEntity> logEntities = getLogs(info);

    try {
      InputStream inputStream = zipAndConvertToInputStream(logEntities);
      return Response.ok().entity(inputStream).header("Content-Disposition", "attachment; filename=" + createTimestampedZipFilename("logs")).build();
    } catch (IOException ioe) {
      throw new ResourceRuntimeException("Failed to get TSA logs", ioe, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  private InputStream zipAndConvertToInputStream(Collection<LogEntity> unsortedLogEntities) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream out = new ZipOutputStream(baos);

    // Sort the logs per source then per timestamp.
    // Once that is done, the sorted logs can be processed sequentially by creating one zip entry each time a
    // new source is encountered and closing the previous one.
    List<LogEntity> sortedLogEntities = new ArrayList<LogEntity>(unsortedLogEntities);
    Collections.sort(sortedLogEntities, new Comparator<LogEntity>() {
      @Override
      public int compare(LogEntity e1, LogEntity e2) {
        int sourceComparison = e1.getSourceId().compareTo(e2.getSourceId());
        if (sourceComparison != 0) {
          return sourceComparison;
        }
        return (int)(e1.getTimestamp() - e2.getTimestamp());
      }
    });

    String lastSourceId = null;
    for (LogEntity logEntity : sortedLogEntities) {
      if (!logEntity.getSourceId().equals(lastSourceId)) {
        if (lastSourceId != null) {
          out.closeEntry();
        }
        out.putNextEntry(new ZipEntry(logEntity.getSourceId().replace(':', '_') + ".txt"));
        lastSourceId = logEntity.getSourceId();
      }
      out.write(logEntity.getMessage().getBytes(Charset.forName("UTF-8")));
    }
    out.closeEntry();
    out.close();

    return new ByteArrayInputStream(baos.toByteArray());
  }

}
