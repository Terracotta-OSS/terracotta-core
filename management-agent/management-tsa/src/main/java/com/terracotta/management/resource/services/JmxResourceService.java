/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.MBeanEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA MBeans.
 *
 * @author Ludovic Orban
 */
public interface JmxResourceService {

  public final static String ATTR_QUERY = "q";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<MBeanEntity> queryMBeans(@Context UriInfo info);

}
