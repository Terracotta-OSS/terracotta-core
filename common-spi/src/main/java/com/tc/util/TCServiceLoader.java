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

package com.tc.util;

import java.util.Collection;
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
    return IMPL == null ? createDefault() : IMPL;
  }
  
  private static Provider createDefault() {
    return new Provider() {
      @Override
      public <T> Collection<? extends T> getImplementations(Class<T> serviceClass, ClassLoader loader) {
        return StreamSupport.stream(ServiceLoader.load(serviceClass, loader).spliterator(), false).collect(toList());
      }
    };
  }
  
  static interface Provider {
    <T> Collection<? extends T> getImplementations(Class<T> serviceClass, ClassLoader loader);
  }
}