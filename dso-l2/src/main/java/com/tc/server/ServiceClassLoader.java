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

import com.tc.classloader.ServiceLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ServiceClassLoader<T> extends ClassLoader {
  
  private final Map<String, Class<? extends T>> cached = new HashMap<>();
  
  public ServiceClassLoader(ClassLoader loader, Class<T>...serviceTypes) {
    super(loader);
    ServiceLocator locator = new ServiceLocator(loader);
    for (Class<T> serviceType : serviceTypes) {
      List<Class<? extends T>> svcs = locator.getImplementations(serviceType);
      loadServiceClasses(svcs);
    }
  }
  
  public void addServiceClass(Class<? extends T> svc) {
    cached.put(svc.getName(), svc);
  }
  
  private void loadServiceClasses(List<Class<? extends T>> svcs) {
    for (Class<? extends T> svc : svcs) {
      cached.put(svc.getName(), svc);
    }
  }

  @Override
  protected Class<? extends T> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<? extends T> get = cached.get(name);
    if (get != null) {
      return get;
    }
    return (Class<? extends T>)super.loadClass(name, resolve);
  }
}
