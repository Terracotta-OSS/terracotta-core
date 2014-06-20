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

import com.terracotta.management.resource.MBeanEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.JmxServiceV2;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA MBeans.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/jmx")
public class JmxResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(JmxResourceServiceImplV2.class);

  private final JmxServiceV2 jmxService;
  private final RequestValidator requestValidator;

  public JmxResourceServiceImplV2() {
    this.jmxService = ServiceLocator.locate(JmxServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_QUERY = "q";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<MBeanEntityV2> queryMBeans(@Context UriInfo info) {
    LOG.debug(String.format("Invoking JmxResourceServiceImplV2.queryMBeans: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String query = qParams.getFirst(ATTR_QUERY);

      return jmxService.queryMBeans(serverNames, query);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA MBeans", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
