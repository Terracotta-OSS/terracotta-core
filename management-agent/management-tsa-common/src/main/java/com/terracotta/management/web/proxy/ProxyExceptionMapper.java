/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.proxy;

import org.glassfish.jersey.server.ContainerRequest;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Proxies a request to a different server.
 *
 * @author Ludovic Orban
 */
@Provider
public class ProxyExceptionMapper implements ExceptionMapper<ProxyException> {

  private final RemoteManagementSource remoteManagementSource = ServiceLocator.locate(RemoteManagementSource.class);

  @Override
  public Response toResponse(ProxyException exception) {
    ContainerRequestContext request = ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.get();
    String activeL2Url = exception.getActiveL2WithMBeansUrl();
    URI uri = request.getUriInfo().getRequestUri();
    String method = request.getMethod();
    URI uriToGo = UriBuilder.fromUri(activeL2Url).path(uri.getPath()).replaceQuery(uri.getQuery()).build();

    if ("GET".equals(method)) {
      return remoteManagementSource.resource(uriToGo).get();
    } else if ("POST".equals(method)) {
      String e = ((ContainerRequest)request).readEntity(String.class);
      return remoteManagementSource.resource(uriToGo).post(Entity.entity(e, request.getMediaType()));
    } else if ("PUT".equals(method)) {
      String e = ((ContainerRequest)request).readEntity(String.class);
      return remoteManagementSource.resource(uriToGo).put(Entity.entity(e, request.getMediaType()));
    } else if ("DELETE".equals(method)) {
      return remoteManagementSource.resource(uriToGo).delete();
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(ExceptionUtils.toErrorEntity(new Exception("Cannot proxy " + method + " HTTP method"))).build();
    }
  }

}
