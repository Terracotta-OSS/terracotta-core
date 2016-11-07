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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.IEntityMessenger.ScheduledToken;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServiceConfiguration;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;


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
    this.messageSink = mock(Sink.class);
    this.consumerID = 1;
    this.messageCodec = mock(MessageCodec.class);
    this.owningEntity = mock(ManagedEntity.class);
    when(this.owningEntity.getCodec()).thenReturn((MessageCodec)this.messageCodec);
    when(this.owningEntity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    this.configuration = mock(ServiceConfiguration.class);
    when(this.configuration.getServiceType()).thenReturn(IEntityMessenger.class);
    
    // Build the timer we will use in the provider.
    this.timeSource = new TestTimeSource(1L);
    this.timer = new SingleThreadedTimer(this.timeSource);
    this.timer.start();
    
    // Build the test subject.
    this.entityMessengerProvider = new EntityMessengerProvider(this.timer);
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
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
  }

  @Test
  public void testSingleDelayedMessage() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message = mock(EntityMessage.class);
    
    long millisBeforeSend = 1000;
    service.messageSelfAfterDelay(message, millisBeforeSend);
    
    // Verify that the message is encoded but not yet enqueued.
    verify(this.messageCodec).encodeMessage(message);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time a little.
    this.timeSource.passTime(1L);
    this.timer.poke();
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time the rest of the way.
    this.timeSource.passTime(millisBeforeSend);
    this.timer.poke();
    
    // Verify that the call did get enqueued.
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTwoDelayedMessages() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message1 = mock(EntityMessage.class);
    EntityMessage message2 = mock(EntityMessage.class);
    
    // Send both messages.
    long millisBeforeSend1 = 1000;
    long millisBeforeSend2 = 2000;
    service.messageSelfAfterDelay(message1, millisBeforeSend1);
    service.messageSelfAfterDelay(message2, millisBeforeSend2);
    
    // Verify that the messages were encoded but not yet enqueued.
    verify(this.messageCodec).encodeMessage(message1);
    verify(this.messageCodec).encodeMessage(message2);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time to the first one.
    this.timeSource.passTime(millisBeforeSend1);
    this.timer.poke();
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
    reset(this.messageSink);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time the rest of the way.
    this.timeSource.passTime(millisBeforeSend2 - millisBeforeSend1);
    this.timer.poke();
    
    // Verify that the call did get enqueued.
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSinglePeriodicMessage() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message = mock(EntityMessage.class);
    
    long millisBetweenSends = 1000;
    service.messageSelfPeriodically(message, millisBetweenSends);
    
    // Verify that the message is encoded but not yet enqueued.
    verify(this.messageCodec).encodeMessage(message);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time until the first invocation.
    this.timeSource.passTime(millisBetweenSends);
    this.timer.poke();
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time a little further.
    reset(this.messageSink);
    this.timeSource.passTime(1L);
    this.timer.poke();
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time to the next invocation.
    this.timeSource.passTime(millisBetweenSends);
    this.timer.poke();
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
  }

  @Test
  public void testCancelledDelayedMessage() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message = mock(EntityMessage.class);
    
    long millisBeforeSend = 1000;
    ScheduledToken token = service.messageSelfAfterDelay(message, millisBeforeSend);
    
    // Verify that the message is encoded but not yet enqueued.
    verify(this.messageCodec).encodeMessage(message);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time a little.
    this.timeSource.passTime(1L);
    this.timer.poke();
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Cancel the message.
    service.cancelTimedMessage(token);
    
    // Advance time the rest of the way.
    this.timeSource.passTime(millisBeforeSend);
    this.timer.poke();
    
    // Verify that the call never happened.
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCancelledPeriodicMessage() throws Exception {
    IEntityMessenger service = this.entityMessengerProvider.getService(this.consumerID, this.owningEntity, this.configuration);
    EntityMessage message = mock(EntityMessage.class);
    
    long millisBetweenSends = 1000;
    ScheduledToken token = service.messageSelfPeriodically(message, millisBetweenSends);
    
    // Verify that the message is encoded but not yet enqueued.
    verify(this.messageCodec).encodeMessage(message);
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time until the first invocation.
    this.timeSource.passTime(millisBetweenSends);
    this.timer.poke();
    verify(this.messageSink).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Advance time a little further.
    reset(this.messageSink);
    this.timeSource.passTime(1L);
    this.timer.poke();
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
    
    // Cancel the message.
    service.cancelTimedMessage(token);
    
    // Advance time to the next invocation.
    this.timeSource.passTime(millisBetweenSends);
    this.timer.poke();
    verify(this.messageSink, never()).addSingleThreaded(any(VoltronEntityMessage.class));
  }


  private static class TestTimeSource implements SingleThreadedTimer.TimeSource {
    private long currentTimeMillis;
    
    public TestTimeSource(long currentTimeMillis) {
      this.currentTimeMillis = currentTimeMillis;
    }
    
    public void passTime(long millis) {
      this.currentTimeMillis += millis;
    }
    
    @Override
    public long currentTimeMillis() {
      return this.currentTimeMillis;
    }
  }
}
