/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.passthrough;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceConfiguration;


/**
 * The provider of PassthroughCommunicatorService, to server-side entities.  It has no meaningful implementation beyond
 * providing that.
 */
public class PassthroughCommunicatorServiceProvider implements PassthroughImplementationProvidedServiceProvider {
  @Override
  public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(new PassthroughCommunicatorService(container));
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // Using Collections.singleton here complains about trying to unify between different containers of different class
    // bindings so doing it manually satisfies the compiler (seems to work in Java8 but not Java6).
    Set<Class<?>> set = new HashSet<Class<?>>();
    set.add(ClientCommunicator.class);
    return set;
  }
}
