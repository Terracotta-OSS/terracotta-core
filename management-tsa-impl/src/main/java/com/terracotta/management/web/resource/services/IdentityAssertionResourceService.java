/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.web.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;

import com.terracotta.management.security.RequestIdentityAsserter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A security resource service that provides identity assertion for downstream consumers.
 *
 * @author brandony
 */
@Path("/assertIdentity")
public final class IdentityAssertionResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(IdentityAssertionResourceService.class);
  private final RequestIdentityAsserter idAsserter;

  public IdentityAssertionResourceService() {
    this.idAsserter = ServiceLocator.locate(RequestIdentityAsserter.class);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIdentity(@Context HttpServletRequest request,
                              @Context HttpServletResponse response) {
    try {
      return Response.ok(idAsserter.assertIdentity(request, response)).build();
    } catch (Exception e) {
      LOG.error("Identity assertion failure!", e);
      throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
    }
  }
}
