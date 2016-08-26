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
package com.tc.services;

import java.util.Collection;
import java.util.Collections;

import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;


/**
 * The built-in provider of IEntityMessenger services.
 * These messages are fed into the general VoltronEntityMessage sink, provided by the server implementation.
 */
public class EntityMessengerProvider implements ImplementationProvidedServiceProvider {
  private final Sink<VoltronEntityMessage> messageSink;

  public EntityMessengerProvider(Sink<VoltronEntityMessage> messageSink) {
    this.messageSink = messageSink;
  }

  @Override
  public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(new EntityMessengerService(this.messageSink, owningEntity));
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IEntityMessenger.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // Do nothing.
  }
}
