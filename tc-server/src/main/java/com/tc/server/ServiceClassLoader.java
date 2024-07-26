/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
public class ServiceClassLoader extends ClassLoader {
  
  private final Map<String, Class<?>> cached = new HashMap<>();

  @SuppressWarnings({"rawtypes","unchecked"})
  public ServiceClassLoader(ServiceLocator loader, Class<?>...serviceTypes) {
    super(loader.getServiceLoader());
    for (Class serviceType : serviceTypes) {
      List<Class<?>> svcs = loader.getImplementations(serviceType);
      loadServiceClasses(svcs);
    }
  }

  @SuppressWarnings({"rawtypes","unchecked"})
  public ServiceClassLoader(ClassLoader loader, Class<?>...serviceTypes) {
    super(loader);
    ServiceLocator locator = new ServiceLocator(loader);
    for (Class serviceType : serviceTypes) {
      List<Class<?>> svcs = locator.getImplementations(serviceType);
      loadServiceClasses(svcs);
    }
  }
  
  public void addServiceClass(Class<?> svc) {
    cached.put(svc.getName(), svc);
  }
  
  private void loadServiceClasses(List<Class<?>> svcs) {
    for (Class<?> svc : svcs) {
      cached.put(svc.getName(), svc);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> get = cached.get(name);
    if (get != null) {
      return get;
    }
    return (Class<?>)super.loadClass(name, resolve);
  }
}
