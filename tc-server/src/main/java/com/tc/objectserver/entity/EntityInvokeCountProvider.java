/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
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
package com.tc.objectserver.entity;

import com.tc.objectserver.api.ManagedEntity;
import com.tc.services.ImplementationProvidedServiceProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.OptionalLong;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.monitoring.EntityInvokeMetrics;

/**
 *
 */
public class EntityInvokeCountProvider implements ImplementationProvidedServiceProvider {

  private final EntityManagerImpl impl;

  public EntityInvokeCountProvider(EntityManagerImpl impl) {
    this.impl = impl;
  }

  @Override
  public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
    if (configuration.getServiceType() == EntityInvokeMetrics.class) {
      return configuration.getServiceType().cast(new EntityInvokeMetrics() {
        @Override
        public long getInvokeCount() {
          return impl.getInvokeCount(null, null).getAsLong();
        }

        @Override
        public OptionalLong getInvokeCountForType(String type) {
          return impl.getInvokeCount(type, null);
        }

        @Override
        public OptionalLong getInvokeCountForId(String type, String name) {
          return impl.getInvokeCount(type, name);
        }
      });
    }
    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(EntityInvokeMetrics.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {

  }

  @Override
  public void serverDidBecomeActive() {

  }
}
