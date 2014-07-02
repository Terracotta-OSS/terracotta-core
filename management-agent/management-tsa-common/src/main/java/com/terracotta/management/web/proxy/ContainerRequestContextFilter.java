/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.proxy;

import org.glassfish.jersey.server.ContainerRequest;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * @author Ludovic Orban
 */
public class ContainerRequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

  public static final ThreadLocal<ContainerRequestContext> CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL = new ThreadLocal<ContainerRequestContext>();

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    ((ContainerRequest)requestContext).bufferEntity();
    CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.remove();
  }

}
