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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServiceConfiguration;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class EntityMessengerProviderTest {
  private Sink<VoltronEntityMessage> messageSink;
  private long consumerID;
  private MessageCodec<EntityMessage, EntityResponse> messageCodec;
  private ManagedEntity owningEntity;
  private ServiceConfiguration<IEntityMessenger> configuration;

  private EntityMessengerProvider entityMessengerProvider;


  // Suppress warnings about mock assignments not having the right generics.
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setUp() throws Exception {
    // Build the underlying components needed by the provider or common to tests.
    this.messageSink = mock(Sink.class);
    this.consumerID = 1;
    this.messageCodec = mock(MessageCodec.class);
    this.owningEntity = mock(ManagedEntity.class);
    when(this.owningEntity.getCodec()).thenReturn((MessageCodec)this.messageCodec);
    when(this.owningEntity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    this.configuration = mock(ServiceConfiguration.class);
    when(this.configuration.getServiceType()).thenReturn(IEntityMessenger.class);
    
    // Build the test subject.
    this.entityMessengerProvider = new EntityMessengerProvider();
    this.entityMessengerProvider.setMessageSink(this.messageSink);
    // Note that we can only serve this service if in active mode.
    this.entityMessengerProvider.serverDidBecomeActive();
  }

  @Test
  public void testBuildServiceOnly() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    Assert.assertNotNull(service);
  }

  @Test
  public void testSendSimpleMessage() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message = mock(EntityMessage.class);
    
    service.messageSelf(message);
    
    // Verify the calls we observed.
    verify(this.messageCodec).encodeMessage(message);
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
  }
}
