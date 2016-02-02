/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.terracotta.config.service.ServiceConfigParser;

/**
 *
 */
public class ServiceClassLoader extends ClassLoader {
  
  private final Map<String, Class<?>> cached;
  
  public ServiceClassLoader(List<Class<? extends ServiceConfigParser>> svcs) {
    Map<String, Class<?>> set = new HashMap<>();
    for (Class<?> svc : svcs) {
      set.put(svc.getName(), svc);
    }
    cached = Collections.unmodifiableMap(set);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> get = cached.get(name);
    if (get != null) {
      return get;
    }
    return super.loadClass(name, resolve);
  }
}
