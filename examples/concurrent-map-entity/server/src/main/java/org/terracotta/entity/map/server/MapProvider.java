/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity.map.server;

import com.tc.classloader.BuiltinService;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 *
 */
@BuiltinService
public class MapProvider implements ServiceProvider {
  
  private final ConcurrentMap<Object, Object> map = new ConcurrentHashMap<>();

  @Override
  public boolean initialize(ServiceProviderConfiguration spc, PlatformConfiguration pc) {
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> sc) {
    if (sc.getServiceType().isAssignableFrom(Map.class)) {
      return (T)map;
    } else {
      return null;
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(Map.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
    map.clear();
  }
  
}
