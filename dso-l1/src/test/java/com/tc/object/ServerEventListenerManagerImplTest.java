package com.tc.object;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tc.exception.TCRuntimeException;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImplTest {

  private ServerEventListenerManagerImpl manager;
  private ServerEventDestination[] destinations;
  private final NodeID remoteNode = new GroupID(1);
  private final TaskRunner mockrunner = mock(TaskRunner.class);

  private static class NormalTimer  implements Timer {
      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        command.run();
        return mock(ScheduledFuture.class);
      }
      @Override
      public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return null;
      }

      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return null;
      }

      @Override
      public void execute(Runnable command) {

      }
      @Override
      public void cancel() {
      }
  }

  private class RougeTimer implements Timer {
    private final ScheduledFuture future = mock(ScheduledFuture.class);

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      command.run();
      try {
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(TimeoutException.class);
      } catch (InterruptedException e) {
        //
      } catch (ExecutionException e) {
        //
      } catch (TimeoutException e) {
        //
      }
      return future;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public void execute(Runnable command) {

    }
    @Override
    public void cancel() {
    }
  }


  @Before
  public void setUp() throws Exception {
    TCProperties properties = TCPropertiesImpl.getProperties();
    properties.setProperty(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_TIMEOUT_INTERVAL, "1");
    manager = new ServerEventListenerManagerImpl(mockrunner);
    when(mockrunner.newTimer()).thenReturn(new NormalTimer());
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


  @Test(expected= TCRuntimeException.class)
  public void testLongRunningDispatch() throws Exception {
    when(mockrunner.newTimer()).thenReturn(new RougeTimer());
    final ServerEvent event1 = new BasicServerEvent(EVICT, "key-1", "cache1");

    manager.registerListener(destinations[0], EnumSet.of(EVICT, PUT));
    manager.dispatch(event1, remoteNode);

  }
}
