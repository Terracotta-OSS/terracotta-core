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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.Collection;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;


public class PassthroughClasspathBuiltinServiceProvider implements PassthroughBuiltInServiceProvider {
  private final ServiceProvider delegate;
  
  public PassthroughClasspathBuiltinServiceProvider(ServiceProvider provider) {
    this.delegate = provider;
  }

  @Override
  public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(this.delegate.getService(consumerID, configuration));
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return delegate.getProvidedServiceTypes();
  }
}
