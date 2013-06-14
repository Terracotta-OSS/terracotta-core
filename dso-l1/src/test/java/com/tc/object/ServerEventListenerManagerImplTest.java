package com.tc.object;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.ServerEventListenerMessageFactory;
import com.tc.object.msg.UnregisterServerEventListenerMessage;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

import java.util.EnumSet;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;
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

  private RegisterServerEventListenerMessage registrationMsgMock;
  private UnregisterServerEventListenerMessage unregistrationMsgMock;
  private ServerEventListenerManagerImpl manager;
  private ServerEventDestination[] destinations;

  @Before
  public void setUp() throws Exception {
    final GroupID groupId = new GroupID(1);
    registrationMsgMock = mock(RegisterServerEventListenerMessage.class);
    unregistrationMsgMock = mock(UnregisterServerEventListenerMessage.class);

    final ServerEventListenerMessageFactory factoryMock = mock(ServerEventListenerMessageFactory.class);
    when(factoryMock.newRegisterServerEventListenerMessage(groupId)).thenReturn(registrationMsgMock);
    when(factoryMock.newUnregisterServerEventListenerMessage(groupId)).thenReturn(unregistrationMsgMock);

    manager = new ServerEventListenerManagerImpl(factoryMock, groupId);

    // destinations
    destinations = new ServerEventDestination[5];
    destinations[0] = createDestination("cache1");
    destinations[1] = createDestination("cache1");
    destinations[2] = createDestination("cache2");
    destinations[3] = createDestination("cache3");
    destinations[4] = createDestination("cache3");

    // register several listeners
    manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
    manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
    manager.registerListener(destinations[1], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
    manager.registerListener(destinations[3], EnumSet.of(PUT));
    manager.registerListener(destinations[4], EnumSet.of(PUT));
  }

  private ServerEventDestination createDestination(final String name) {
    return when(mock(ServerEventDestination.class).getDestinationName()).thenReturn(name).getMock();
  }

  @Test
  public void testShouldRegisterUnregisterAndDeliverEvents() {
    final NodeID remoteNode = new GroupID(1);
    final ServerEvent event1 = new BasicServerEvent(EVICT, 1, "cache1");
    final ServerEvent event2 = new BasicServerEvent(PUT, 2, "cache3");
    final ServerEvent event3 = new BasicServerEvent(EXPIRE, 3, "cache2");

    // two events should be delivered
    manager.dispatch(event1, remoteNode);
    manager.dispatch(event2, remoteNode);
    // exception due to non-existent mapping
    try {
      manager.dispatch(event3, remoteNode);
      fail();
    } catch (IllegalStateException e) {
      // just as planned
    }

    // remove several mappings
    manager.unregisterListener(destinations[3]);
    manager.unregisterListener(destinations[4]);

    manager.dispatch(event2, remoteNode);

    // verify invocations
    verify(registrationMsgMock, times(6)).send();
    verify(destinations[1]).handleServerEvent(event1);
    verify(destinations[3]).handleServerEvent(event2);
    verify(destinations[4]).handleServerEvent(event2);
    verify(destinations[0], never()).handleServerEvent(any(ServerEvent.class));
    verify(destinations[2], never()).handleServerEvent(any(ServerEvent.class));
    verify(unregistrationMsgMock, times(1)).send();
  }

  @Test
  public void testShouldReregisterListenersOnUnpause() {
    manager.unpause(any(NodeID.class), 0); // don't care about params
    verify(registrationMsgMock, times(9)).send(); // 6 initial registrations + 3 re-registrations on reconnect
  }

}
