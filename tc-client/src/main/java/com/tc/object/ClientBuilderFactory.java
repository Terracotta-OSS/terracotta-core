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
package com.tc.object;

import com.tc.util.ManagedServiceLoader;

import java.util.Properties;

public interface ClientBuilderFactory {

  static <T> T get(Class<T> type) {

    T finalFactory = null;

    for (T factory : ManagedServiceLoader.loadServices(type, ClientBuilderFactory.class.getClassLoader())) {
      if (finalFactory == null) {
        finalFactory = factory;
      } else {
        throw new RuntimeException("Found multiple implementations of " + type.getName());
      }
    }
    
    return finalFactory;
  }

  ClientBuilder create(Properties connectionProperties);
}
