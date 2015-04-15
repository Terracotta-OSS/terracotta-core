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