/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
