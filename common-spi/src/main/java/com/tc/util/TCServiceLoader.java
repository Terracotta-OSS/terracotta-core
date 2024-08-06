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
package com.tc.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import static java.util.stream.Collectors.toList;
import java.util.stream.StreamSupport;



/**
 */
public class TCServiceLoader {
  
  private static Provider IMPL;
  
  static void setImplementation(Provider impl) {
    IMPL = impl;
  }

  public static <T> Collection<? extends T> loadServices(Class<T> serviceClass, ClassLoader loader) {
    return getImpl().getImplementations(serviceClass, loader);
  }

  public static <T> Collection<? extends T> loadServices(Class<T> serviceClass) {
    return getImpl().getImplementations(serviceClass, serviceClass.getClassLoader());
  }
  
  private static Provider getImpl() {
    return IMPL == null ? loadServiceProvider() : IMPL;
  }
  
  private static Provider createDefault() {
    return new Provider() {
      @Override
      public <T> Collection<? extends T> getImplementations(Class<T> serviceClass, ClassLoader loader) {
        return StreamSupport.stream(ServiceLoader.load(serviceClass, loader).spliterator(), false).collect(toList());
      }
    };
  }
  
  private static Provider loadServiceProvider() {
    ServiceLoader<Provider> p = ServiceLoader.load(Provider.class);
    Iterator<Provider>  ip = p.iterator();
    if (ip.hasNext()) {
      IMPL = ip.next();
      return IMPL;
    } else {
      IMPL = createDefault();
    }
    return IMPL;
  }
  
  static interface Provider {
    <T> Collection<? extends T> getImplementations(Class<T> serviceClass, ClassLoader loader);
  }
}
