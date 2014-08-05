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

    // only add the "Accept-Encoding" header on the proxy request when the original request contains
    // them, otherwise we're going to stream compressed data to a client which may not support that.
    String acceptEncoding = request.getHeaders().getFirst("Accept-Encoding");
    boolean compress = acceptEncoding != null && (acceptEncoding.contains("gzip") || acceptEncoding.contains("deflate"));

    if ("GET".equals(method)) {
      return buildResponse(remoteManagementSource.resource(uriToGo, compress).get());
    } else if ("POST".equals(method)) {
      byte[] e = ((ContainerRequest)request).readEntity(byte[].class);
      return buildResponse(remoteManagementSource.resource(uriToGo, compress).post(Entity.entity(e, request.getMediaType())));
    } else if ("PUT".equals(method)) {
      byte[] e = ((ContainerRequest)request).readEntity(byte[].class);
      return buildResponse(remoteManagementSource.resource(uriToGo, compress).put(Entity.entity(e, request.getMediaType())));
    } else if ("DELETE".equals(method)) {
      return buildResponse(remoteManagementSource.resource(uriToGo, compress).delete());
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(ExceptionUtils.toErrorEntity(new Exception("Cannot proxy " + method + " HTTP method"))).build();
    }
  }

  private Response buildResponse(Response response) {
    return Response.fromResponse(response)
        .entity(response.readEntity(byte[].class))
        .build();
  }

}
