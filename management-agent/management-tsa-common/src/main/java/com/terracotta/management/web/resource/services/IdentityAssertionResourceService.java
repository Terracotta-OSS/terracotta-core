/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.web.resource.services;

import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.security.RequestIdentityAsserter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
      throw new ResourceRuntimeException("Identity assertion failure", e, Response.Status.UNAUTHORIZED.getStatusCode());
    }
  }
}
