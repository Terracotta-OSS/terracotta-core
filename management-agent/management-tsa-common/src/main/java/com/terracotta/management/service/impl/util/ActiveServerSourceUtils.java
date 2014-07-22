/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.terracotta.management.resource.ErrorEntity;

import com.terracotta.management.web.proxy.ProxyException;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class ActiveServerSourceUtils {

  public static void proxyClientRequest(List<String> activeL2Urls) throws ProxyException, WebApplicationException {
    if (activeL2Urls.isEmpty()) {
      ErrorEntity errorEntity = new ErrorEntity();
      errorEntity.setError("No active coordinator");
      errorEntity.setDetails("No server is in the ACTIVE-COORDINATOR state, try again later.");
      throw new WebApplicationException(Response.status(404).entity(errorEntity).build());
    }
    Collections.shuffle(activeL2Urls);
    String activeL2Url = activeL2Urls.get(0);
    throw new ProxyException(activeL2Url);
  }

}
