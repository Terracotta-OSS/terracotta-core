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
package com.tc.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;

/**
 *
 * @author mscott
 */
public class TestServiceProvider implements ServiceProvider {

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(new TestService() {});
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(TestService.class);
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // nothing to do
  }
  
}
