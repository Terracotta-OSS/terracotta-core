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

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.MessageCodec;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.ActiveServerEntity;


public class EntityMessengerServiceTest {
  @Test
  public void testExplicitRetirement() throws Exception {
    // Thank god I can steal Jeff's mocks.
    ISimpleTimer timer = mock(ISimpleTimer.class);
    when(timer.addPeriodic(any(), anyLong(), anyLong())).thenReturn(1L);
    when(timer.addDelayed(any(), anyLong())).thenReturn(1L);
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.isDestroyed()).thenReturn(true);
    RetirementManager retirementManager = mock(RetirementManager.class);
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService service = new EntityMessengerService(sink, entity, true);
    when(entity.isDestroyed()).thenReturn(false);
    ActiveServerEntity ae = mock(ActiveServerEntity.class);
    service.entityCreated(entity);

    EntityMessage deferrableMessage = mock(EntityMessage.class);
    EntityMessage futureMessage = mock(EntityMessage.class);
    ExplicitRetirementHandle handle = service.deferRetirement("test", deferrableMessage, futureMessage);

    // verify it was deferred
    verify(retirementManager).deferRetirement(deferrableMessage, futureMessage);
    handle.release();
    // verify that got scheduled.
    verify(sink).addToSink(any());
  }
  
  @Test
  public void testEarlySend() throws Exception {
    ISimpleTimer timer = mock(ISimpleTimer.class);
    when(timer.addPeriodic(any(), anyLong(), anyLong())).thenReturn(1L);
    when(timer.addDelayed(any(), anyLong())).thenReturn(1L);
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.isDestroyed()).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    @SuppressWarnings("rawtypes")
    MessageCodec codec = mock(MessageCodec.class);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService service = new EntityMessengerService(sink, entity, true);
    // now adding listener in provider so do it manually
    entity.addLifecycleListener(service);
    // Verify that the service was registered to be told when the entity activates.
    verify(entity).addLifecycleListener(service);

    // messageSelf before create is finished
    EntityMessage delayMessage = mock(EntityMessage.class);
    service.messageSelf(delayMessage);

    verify(sink).addToSink(any(VoltronEntityMessage.class));
  }
}
