/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management;

import org.glassfish.jersey.media.sse.SseFeature;

import com.terracotta.management.web.proxy.ProxyExceptionMapper;
import com.terracotta.management.web.proxy.ContainerRequestContextFilter;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ApplicationTsa extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>();
    s.add(SseFeature.class);
    s.add(ProxyExceptionMapper.class);
    s.add(ContainerRequestContextFilter.class);
    ServiceLoader<ApplicationTsaService> loader = ServiceLoader.load(ApplicationTsaService.class);
    for (ApplicationTsaService applicationTsaService : loader) {
      s.addAll(applicationTsaService.getResourceClasses());
    }
    return s;
  }

}