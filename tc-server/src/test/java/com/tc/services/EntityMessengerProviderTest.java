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

import org.junit.After;
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
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;


public class EntityMessengerProviderTest {
  private Sink<VoltronEntityMessage> messageSink;
  private long consumerID;
  private MessageCodec<EntityMessage, EntityResponse> messageCodec;
  private ManagedEntity owningEntity;
  private ServiceConfiguration<IEntityMessenger> configuration;
  private TestTimeSource timeSource;
  private SingleThreadedTimer timer;

  private EntityMessengerProvider entityMessengerProvider;


  // Suppress warnings about mock assignments not having the right generics.
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setUp() throws Exception {
    // Build the underlying components needed by the provider or common to tests.
    ServerEnv.setDefaultServer(mock(Server.class));
    this.messageSink = mock(Sink.class);
    this.consumerID = 1;
    this.messageCodec = mock(MessageCodec.class);
    when(this.messageCodec.encodeMessage(any())).thenReturn(new byte[0]);
    this.owningEntity = mock(ManagedEntity.class);
    when(this.owningEntity.getCodec()).thenReturn((MessageCodec)this.messageCodec);
    when(this.owningEntity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    this.configuration = mock(ServiceConfiguration.class);
    when(this.configuration.getServiceType()).thenReturn(IEntityMessenger.class);
    
    // Build the timer we will use in the provider.
    this.timeSource = new TestTimeSource(1L);
    this.timer = new SingleThreadedTimer(this.timeSource, null);
    this.timer.start();
    
    // Build the test subject.
    this.entityMessengerProvider = new EntityMessengerProvider();
    this.entityMessengerProvider.setMessageSink(this.messageSink);
    // Note that we can only serve this service if in active mode.
    this.entityMessengerProvider.serverDidBecomeActive();
  }

  @After
  public void tearDown() throws Exception {
    this.timer.stop();
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
    verify(this.messageSink).addToSink(any(VoltronEntityMessage.class));
  }
}
