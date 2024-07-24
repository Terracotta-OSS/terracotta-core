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

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.monitoring.PlatformService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class PassthroughPlatformServiceProvider implements PassthroughImplementationProvidedServiceProvider {
    private final PassthroughPlatformService passthroughPlatformService;

    public PassthroughPlatformServiceProvider(PassthroughClusterControl passthroughClusterControl, PassthroughServer passthroughServer) {
        this.passthroughPlatformService = new PassthroughPlatformService(passthroughClusterControl, passthroughServer);
    }


    @Override
    public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
        return configuration.getServiceType().cast(passthroughPlatformService);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(PlatformService.class);
        return set;
    }
}
