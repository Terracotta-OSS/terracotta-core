/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA shutdown.
 *
 * @author Ludovic Orban
 */
public interface ShutdownResourceService {

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  boolean shutdown(@Context UriInfo info);

}
