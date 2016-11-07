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
import com.tc.util.Assert;


/**
 * The built-in provider of IEntityMessenger services.
 * These messages are fed into the general VoltronEntityMessage sink, provided by the server implementation.
 */
public class EntityMessengerProvider implements ImplementationProvidedServiceProvider {
  private final SingleThreadedTimer timer;
  private Sink<VoltronEntityMessage> messageSink;
  private boolean serverIsActive;

  public EntityMessengerProvider(SingleThreadedTimer timer) {
    Assert.assertNotNull(timer);
    
    this.timer = timer;
  }

  @Override
  public <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
    Assert.assertNotNull(this.messageSink);
    // This service can't be used for fake entities (this is a bug, not a usage error, since the only fake entities are internal).
    Assert.assertNotNull(owningEntity);
    T service = null;
    if (this.serverIsActive) {
      service = configuration.getServiceType().cast(new EntityMessengerService(this.timer, this.messageSink, owningEntity));
    }
    return service;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IEntityMessenger.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // Do nothing.
  }

  @Override
  public void serverDidBecomeActive() {
    Assert.assertNotNull(this.messageSink);
    // The entity messenger service is only enabled when we are active.
    this.serverIsActive = true;
  }

  public void setMessageSink(Sink<VoltronEntityMessage> messageSink) {
    Assert.assertNotNull(messageSink);
    this.messageSink = messageSink;
  }
}
