/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.services;

import com.tc.objectserver.api.ManagedEntity;
import com.tc.server.TCServer;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.monitoring.PlatformService;

import java.util.Collection;
import java.util.Collections;


public class PlatformServiceProvider implements ImplementationProvidedServiceProvider {

    private final PlatformServiceImpl platformService;

    public PlatformServiceProvider(TCServer tcServer) {
        this.platformService = new PlatformServiceImpl(tcServer);
    }


    @Override
    public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
        return configuration.getServiceType().cast(platformService);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
        return Collections.singleton(PlatformService.class);
    }

    @Override
    public void clear() throws ServiceProviderCleanupException {
        //no-op
    }

    @Override
    public void serverDidBecomeActive() {
      // The platform service works the same way whether active or passive - ignore.
    }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    StateDumpCollector dumpCollector = stateDumpCollector.subStateDumpCollector(getClass().getCanonicalName());
    StateDumpCollector dump = dumpCollector.subStateDumpCollector(platformService.getClass()
        .getSimpleName());
    platformService.addStateTo(dump);
  }
}
