package com.tc.object;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.ServerEventListenerMessageFactory;
import com.tc.object.msg.UnregisterServerEventListenerMessage;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImplTest {

  private RegisterServerEventListenerMessage registrationMsgMock;
  private UnregisterServerEventListenerMessage unregistrationMsgMock;
  private ServerEventListenerManagerImpl manager;
  private ServerEventDestination[] destinations;
  private final NodeID remoteNode = new GroupID(1);

  @Before
  public void setUp() throws Exception {
    final GroupID groupId = new GroupID(1);
    registrationMsgMock = mock(RegisterServerEventListenerMessage.class);
    unregistrationMsgMock = mock(UnregisterServerEventListenerMessage.class);

    final ServerEventListenerMessageFactory factoryMock = mock(ServerEventListenerMessageFactory.class);
    when(factoryMock.newRegisterServerEventListenerMessage(groupId)).thenReturn(registrationMsgMock);
    when(factoryMock.newUnregisterServerEventListenerMessage(groupId)).thenReturn(unregistrationMsgMock);

    manager = new ServerEventListenerManagerImpl();

    // destinations
    destinations = new ServerEventDestination[5];
    destinations[0] = createDestination("cache1");
    destinations[1] = createDestination("cache1");
    destinations[2] = createDestination("cache2");
    destinations[3] = createDestination("cache3");
    destinations[4] = createDestination("cache3");
  }

  private ServerEventDestination createDestination(final String name) {
    return when(mock(ServerEventDestination.class).getDestinationName()).thenReturn(name).getMock();
  }
  //
  // @Test
  // public void testMustProperlyRouteEventsToRegisteredListeners() {
  // final ServerEvent event1 = new BasicServerEvent(EVICT, "key-1", "cache1");
  // final ServerEvent event2 = new BasicServerEvent(PUT, "key-2", "cache3");
  // final ServerEvent event3 = new BasicServerEvent(REMOVE, "key-3", "cache2");
  //
  // // register listeners
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  // manager.registerListener(destinations[1], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
  // manager.registerListener(destinations[3], EnumSet.of(PUT));
  // manager.registerListener(destinations[4], EnumSet.of(PUT));
  //
  // // two events should be delivered
  // manager.dispatch(event1, remoteNode);
  // manager.dispatch(event2, remoteNode);
  // manager.dispatch(event3, remoteNode);
  //
  // // verify invocations
  // verify(registrationMsgMock, times(6)).send();
  // verify(destinations[1]).handleServerEvent(event1);
  // verify(destinations[2]).handleServerEvent(event3);
  // verify(destinations[3]).handleServerEvent(event2);
  // verify(destinations[4]).handleServerEvent(event2);
  // verify(destinations[0], never()).handleServerEvent(any(ServerEvent.class));
  // verify(unregistrationMsgMock, never()).send();
  // }
  //
  // @Test
  // public void testMustUpdateRoutingOnUnregistration() {
  // final ServerEvent event1 = new BasicServerEvent(EXPIRE, "key-1", "cache1");
  // final ServerEvent event2 = new BasicServerEvent(PUT, "key-2", "cache1");
  // final ServerEvent event3 = new BasicServerEvent(EVICT, "key-3", "cache2");
  // final ServerEvent event4 = new BasicServerEvent(PUT, "key-5", "cache3");
  //
  // // register listeners
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  // manager.registerListener(destinations[1], EnumSet.of(EVICT, PUT));
  // manager.registerListener(destinations[2], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
  // manager.registerListener(destinations[3], EnumSet.of(PUT));
  // manager.registerListener(destinations[4], EnumSet.of(PUT));
  //
  // // remove several mappings
  // manager.unregisterListener(destinations[0], EnumSet.of(PUT));
  // manager.unregisterListener(destinations[2], EnumSet.of(EVICT, PUT, REMOVE));
  // manager.unregisterListener(destinations[3], EnumSet.of(PUT, REMOVE));
  //
  // manager.dispatch(event1, remoteNode);
  // manager.dispatch(event2, remoteNode);
  // try {
  // manager.dispatch(event3, remoteNode);
  // fail();
  // } catch (IllegalStateException justAsPlanned) { // mapping not found
  // }
  // manager.dispatch(event4, remoteNode);
  //
  // // verify invocations
  // verify(registrationMsgMock, times(6)).send();
  // verify(destinations[0]).handleServerEvent(event1);
  // verify(destinations[0], never()).handleServerEvent(event2);
  // verify(destinations[1]).handleServerEvent(event2);
  // verify(destinations[2], never()).handleServerEvent(any(ServerEvent.class));
  // verify(destinations[3], never()).handleServerEvent(any(ServerEvent.class));
  // verify(destinations[4]).handleServerEvent(event4);
  // verify(unregistrationMsgMock, times(3)).send();
  // }
  //
  // @Test
  // public void testMustNotRegisterSameListenersTwice() {
  // // same listeners
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE));
  //
  // // register only once
  // verify(registrationMsgMock, times(1)).send();
  // }
  //
  // @Test
  // public void testMustNotUnregisterSameListenersTwice() {
  // // register listeners
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  //
  // // same listeners
  // manager.unregisterListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  // manager.unregisterListener(destinations[0], EnumSet.of(EXPIRE));
  //
  // // unregister only once
  // verify(unregistrationMsgMock, times(1)).send();
  // }
  //
  // @Test(expected = IllegalStateException.class)
  // public void testMustFailOnNonExistentMapping() {
  // // register listeners
  // manager.registerListener(destinations[2], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
  //
  // final ServerEvent event1 = new BasicServerEvent(EXPIRE, "key-1", "cache2");
  // // exception due to non-existent mapping
  // manager.dispatch(event1, remoteNode);
  // }
  //
  // @Test
  // public void testMustFailOnInvalidParams() {
  // final ServerEvent event1 = new BasicServerEvent(EXPIRE, 3, "cache2");
  // // exception due to non-existent mapping
  // try {
  // manager.dispatch(null, remoteNode);
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  //
  // try {
  // manager.dispatch(event1, null);
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  //
  // try {
  // manager.registerListener(null, EnumSet.of(EVICT));
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  //
  // try {
  // manager.registerListener(destinations[0], Sets.<ServerEventType>newHashSet());
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  //
  // try {
  // manager.unregisterListener(null, EnumSet.of(EVICT));
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  //
  // try {
  // manager.unregisterListener(destinations[0], Sets.<ServerEventType>newHashSet());
  // fail();
  // } catch (Exception justAsPlanned) {
  // }
  // }
  //
  // @Test
  // public void testMustReregisterListenersOnUnpause() {
  // // register listeners
  // manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
  // manager.registerListener(destinations[1], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(EVICT));
  // manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
  // manager.registerListener(destinations[3], EnumSet.of(PUT));
  // manager.registerListener(destinations[4], EnumSet.of(PUT));
  //
  // manager.unpause(remoteNode, 0); // don't care about params
  //
  // verify(registrationMsgMock, times(9)).send(); // 6 initial registrations + 3 re-registrations on reconnect
  // }

}
