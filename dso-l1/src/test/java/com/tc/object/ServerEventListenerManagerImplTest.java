package com.tc.object;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.tc.net.GroupID;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.ServerEventListenerMessageFactory;
import com.tc.object.msg.ServerEventMessage;
import com.tc.object.msg.UnregisterServerEventListenerMessage;

import java.util.EnumSet;

import static com.tc.object.ServerEventType.EVICT;
import static com.tc.object.ServerEventType.EXPIRE;
import static com.tc.object.ServerEventType.PUT;
import static com.tc.object.ServerEventType.REMOVE;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImplTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testShouldRegisterUnregisterAndDeliverEvents() {
    final GroupID groupId = new GroupID(1);
    final RegisterServerEventListenerMessage registrationMsgMock = mock(RegisterServerEventListenerMessage.class);
    final UnregisterServerEventListenerMessage unregistrationMsgMock = mock(UnregisterServerEventListenerMessage.class);

    final ServerEventListenerMessageFactory factoryMock = mock(ServerEventListenerMessageFactory.class);
    when(factoryMock.newRegisterServerEventListenerMessage(groupId)).thenReturn(registrationMsgMock);
    when(factoryMock.newUnregisterServerEventListenerMessage(groupId)).thenReturn(unregistrationMsgMock);

    final ServerEventListenerManagerImpl manager = new ServerEventListenerManagerImpl(factoryMock, groupId);

    // event message 1
    final ServerEventMessage eventMsgMock1 = mock(ServerEventMessage.class);
    when(eventMsgMock1.getDestinationName()).thenReturn("cache1");
    when(eventMsgMock1.getKey()).thenReturn(1);
    when(eventMsgMock1.getType()).thenReturn(EVICT);

    // event message 2
    final ServerEventMessage eventMsgMock2 = mock(ServerEventMessage.class);
    when(eventMsgMock2.getDestinationName()).thenReturn("cache3");
    when(eventMsgMock2.getKey()).thenReturn(2);
    when(eventMsgMock2.getType()).thenReturn(PUT);

    // event message 3
    final ServerEventMessage eventMsgMock3 = mock(ServerEventMessage.class);
    when(eventMsgMock3.getDestinationName()).thenReturn("cache2");
    when(eventMsgMock3.getKey()).thenReturn(3);
    when(eventMsgMock3.getType()).thenReturn(EXPIRE);

    // destinations
    final ServerEventDestination asm1 = mock(ServerEventDestination.class);
    when(asm1.getDestinationName()).thenReturn("cache1");
    final ServerEventDestination asm2 = mock(ServerEventDestination.class);
    when(asm2.getDestinationName()).thenReturn("cache1");
    final ServerEventDestination asm3 = mock(ServerEventDestination.class);
    when(asm3.getDestinationName()).thenReturn("cache2");
    final ServerEventDestination asm4 = mock(ServerEventDestination.class);
    when(asm4.getDestinationName()).thenReturn("cache3");
    final ServerEventDestination asm5 = mock(ServerEventDestination.class);
    when(asm5.getDestinationName()).thenReturn("cache3");

    // register several listeners
    manager.registerListener(asm1, EnumSet.of(EXPIRE, PUT));
    manager.registerListener(asm1, EnumSet.of(EXPIRE, PUT));
    manager.registerListener(asm2, EnumSet.of(EVICT));
    manager.registerListener(asm3, EnumSet.of(EVICT));
    manager.registerListener(asm3, EnumSet.of(PUT, REMOVE));
    manager.registerListener(asm4, EnumSet.of(PUT));
    manager.registerListener(asm5, EnumSet.of(PUT));
    // two events should be delivered
    manager.dispatch(eventMsgMock1);
    manager.dispatch(eventMsgMock2);
    // exception due to non-existent mapping
    try {
      manager.dispatch(eventMsgMock3);
      fail();
    } catch (IllegalStateException e) {
      // just as planned
    }

    // remove several mappings
    manager.unregisterListener(asm4);
    manager.unregisterListener(asm5);

    manager.dispatch(eventMsgMock2);

    // verify invocations
    verify(registrationMsgMock, times(6)).send();
    verify(asm2).handleServerEvent(EVICT, 1);
    verify(asm4).handleServerEvent(PUT, 2);
    verify(asm5).handleServerEvent(PUT, 2);
    verify(asm1, never()).handleServerEvent((ServerEventType)any(), any());
    verify(asm3, never()).handleServerEvent((ServerEventType)any(), any());
    verify(unregistrationMsgMock, times(1)).send();
  }

  @Test
  @Ignore
  public void testUnpause() {

  }

}
