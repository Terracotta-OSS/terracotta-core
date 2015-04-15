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
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.JmxService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
@Path("/agents/jmx")
public class JmxResourceServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(JmxResourceServiceImpl.class);

  private final JmxService jmxService;
  private final RequestValidator requestValidator;

  public JmxResourceServiceImpl() {
    this.jmxService = ServiceLocator.locate(JmxService.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_QUERY = "q";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<MBeanEntity> queryMBeans(@Context UriInfo info) {
    LOG.debug(String.format("Invoking JmxResourceServiceImpl.queryMBeans: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String query = qParams.getFirst(ATTR_QUERY);

      return jmxService.queryMBeans(serverNames, query);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA MBeans", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
