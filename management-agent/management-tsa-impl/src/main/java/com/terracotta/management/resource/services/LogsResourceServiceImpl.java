/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.LogsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
      LOG.error("Failed to get TSA logs.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to get TSA logs: " + see.getCause().getClass().getName() + ": " + see.getCause()
                  .getMessage()).build());
    }
  }

}
