package com.tc.object;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.tc.exception.TCNotRunningException;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.EnumSet;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImplTest {

  private ServerEventListenerManagerImpl manager;
  private ServerEventDestination[] destinations;
  private final NodeID remoteNode = new GroupID(1);

  @Before
  public void setUp() throws Exception {
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

  @Test
  public void testMustProperlyRouteEventsToRegisteredListeners() {
    final ServerEvent event1 = new BasicServerEvent(EVICT, "key-1", "cache1");
    final ServerEvent event2 = new BasicServerEvent(PUT, "key-2", "cache3");
    final ServerEvent event3 = new BasicServerEvent(REMOVE, "key-3", "cache2");

    // register listeners
    manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
    manager.registerListener(destinations[1], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
    manager.registerListener(destinations[3], EnumSet.of(PUT));
    manager.registerListener(destinations[4], EnumSet.of(PUT));

    // two events should be delivered
    manager.dispatch(event1, remoteNode);
    manager.dispatch(event2, remoteNode);
    manager.dispatch(event3, remoteNode);

    // verify invocations
    verify(destinations[1]).handleServerEvent(event1);
    verify(destinations[2]).handleServerEvent(event3);
    verify(destinations[3]).handleServerEvent(event2);
    verify(destinations[4]).handleServerEvent(event2);
    verify(destinations[0], never()).handleServerEvent(any(ServerEvent.class));
  }

  @Test
  public void testMustUpdateRoutingOnUnregistration() {
    final ServerEvent event1 = new BasicServerEvent(EXPIRE, "key-1", "cache1");
    final ServerEvent event2 = new BasicServerEvent(PUT, "key-2", "cache1");
    final ServerEvent event3 = new BasicServerEvent(EVICT, "key-3", "cache2");
    final ServerEvent event4 = new BasicServerEvent(PUT, "key-5", "cache3");

    // register listeners
    manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
    manager.registerListener(destinations[1], EnumSet.of(EVICT, PUT));
    manager.registerListener(destinations[2], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
    manager.registerListener(destinations[3], EnumSet.of(PUT));
    manager.registerListener(destinations[4], EnumSet.of(PUT));

    // remove several mappings
    manager.unregisterListener(destinations[0], EnumSet.of(PUT));
    manager.unregisterListener(destinations[2], EnumSet.of(EVICT, PUT, REMOVE));
    manager.unregisterListener(destinations[3], EnumSet.of(PUT, REMOVE));

    manager.dispatch(event1, remoteNode);
    manager.dispatch(event2, remoteNode);
    manager.dispatch(event3, remoteNode);
    manager.dispatch(event4, remoteNode);

    // verify invocations
    verify(destinations[0]).handleServerEvent(event1);
    verify(destinations[0], never()).handleServerEvent(event2);
    verify(destinations[1]).handleServerEvent(event2);
    verify(destinations[2], never()).handleServerEvent(any(ServerEvent.class));
    verify(destinations[3], never()).handleServerEvent(any(ServerEvent.class));
    verify(destinations[4]).handleServerEvent(event4);
  }


  @Test
  public void testNoRouteDestinationlessEvent() {
    // register listeners
    manager.registerListener(destinations[2], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));

    final ServerEvent event1 = new BasicServerEvent(EXPIRE, "key-1", "cache2");
    manager.dispatch(event1, remoteNode);

    for (ServerEventDestination destination : destinations) {
      verify(destination, never()).handleServerEvent(any(ServerEvent.class));
    }
  }

  @Test
  public void testMustFailOnInvalidParams() {
    final ServerEvent event1 = new BasicServerEvent(EXPIRE, 3, "cache2");
    // exception due to non-existent mapping
    try {
      manager.dispatch(null, remoteNode);
      fail();
    } catch (Exception justAsPlanned) {
    }

    try {
      manager.dispatch(event1, null);
      fail();
    } catch (Exception justAsPlanned) {
    }

    try {
      manager.registerListener(null, EnumSet.of(EVICT));
      fail();
    } catch (Exception justAsPlanned) {
    }

    try {
      manager.registerListener(destinations[0], Sets.<ServerEventType> newHashSet());
      fail();
    } catch (Exception justAsPlanned) {
    }

    try {
      manager.unregisterListener(null, EnumSet.of(EVICT));
      fail();
    } catch (Exception justAsPlanned) {
    }

    try {
      manager.unregisterListener(destinations[0], Sets.<ServerEventType> newHashSet());
      fail();
    } catch (Exception justAsPlanned) {
    }
  }

  @Test
  public void testMustReregisterListenersOnUnpause() {
    // register listeners
    manager.registerListener(destinations[0], EnumSet.of(EXPIRE, PUT));
    manager.registerListener(destinations[1], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(EVICT));
    manager.registerListener(destinations[2], EnumSet.of(PUT, REMOVE));
    manager.registerListener(destinations[3], EnumSet.of(PUT));
    manager.registerListener(destinations[4], EnumSet.of(PUT));

    manager.unpause(remoteNode, 0); // don't care about params

    verify(destinations[0]).resendEventRegistrations();
    verify(destinations[1]).resendEventRegistrations();
    verify(destinations[2]).resendEventRegistrations();
    verify(destinations[3]).resendEventRegistrations();
  }

  @Test
  public void testUnpauseWhenShutDown() throws Exception {
    doThrow(new TCNotRunningException()).when(destinations[0]).resendEventRegistrations();
    // Should not throw
    manager.unpause(remoteNode, 0);
  }
}
