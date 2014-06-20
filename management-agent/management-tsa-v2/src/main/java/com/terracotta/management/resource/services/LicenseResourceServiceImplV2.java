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

import com.terracotta.management.resource.LicenseEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.LicenseServiceV2;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA license properties
 * 
 * @author Hung Huynh
 */
@Path("/v2/agents/licenseProperties")
public class LicenseResourceServiceImplV2 {

  private static final Logger           LOG = LoggerFactory.getLogger(LicenseResourceServiceImplV2.class);

  private final RequestValidator        requestValidator;
  private final LicenseServiceV2        licenseService;

  public LicenseResourceServiceImplV2() {
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
    this.licenseService = ServiceLocator.locate(LicenseServiceV2.class);
  }

  /**
   * Get a {@code Collection} of {@link LicenseServiceV2} objects
   * 
   * @return a collection of {@link LicenseServiceV2} objects.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<LicenseEntityV2> getLicenseProperties(@Context
  UriInfo info) {
    LOG.debug(String.format("Invoking LicenseResourceServiceImplV2.getLicenseProperties: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "serverNames");
      return licenseService.getLicenseProperties(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get license properties", see,
                                         Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
