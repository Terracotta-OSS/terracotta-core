/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.terracotta.management.resource.ErrorEntity;

import com.terracotta.management.web.proxy.ProxyException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class L1MBeansSourceUtils {

  public static void proxyClientRequest(String activeL2WithMBeansUrls) throws ProxyException, WebApplicationException {
    if (activeL2WithMBeansUrls == null) {
      ErrorEntity errorEntity = new ErrorEntity();
      errorEntity.setError("No management coordinator");
      errorEntity.setDetails("No server is in the ACTIVE-COORDINATOR state in the coordinator group, try again later.");
      throw new WebApplicationException(Response.status(400).entity(errorEntity).build());
    }
    throw new ProxyException(activeL2WithMBeansUrls);
  }

}
