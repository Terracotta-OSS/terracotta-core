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
package com.tc.objectserver.persistence;

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.persistence.IPlatformPersistence;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class NullPlatformStorageServiceProvider implements ServiceProvider, StateDumpable {
    private final Map<Long, NullPlatformPersistentStorage> providers = new ConcurrentHashMap<>();


    @Override
    public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
        return true;
    }

    @Override
    public <T> T getService(long entityID, ServiceConfiguration<T> serviceConfiguration) {
        providers.putIfAbsent(entityID, new NullPlatformPersistentStorage());
        return serviceConfiguration.getServiceType().cast(providers.get(entityID));
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
      return Collections.singleton(IPlatformPersistence.class);
    }

    public void close() throws IOException {
        providers.clear();
    }

    @Override
    public void prepareForSynchronization() throws ServiceProviderCleanupException {
        providers.clear();
    }

    @Override
    public void addStateTo(StateDumpCollector stateDumpCollector) {
        for (Map.Entry<Long, NullPlatformPersistentStorage> entry : providers.entrySet()) {
            entry.getValue().addStateTo(stateDumpCollector.subStateDumpCollector(String.valueOf(entry.getKey())));
        }
    }
}
