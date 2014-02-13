package com.tc.objectserver.event;

import static org.mockito.Mockito.mock;

import org.junit.Before;

import com.tc.net.ClientID;
import com.tc.object.net.DSOChannelManager;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventBufferTest {

  private final ClientID clientId1 = new ClientID(1L);
  private final ClientID clientId2 = new ClientID(2L);
  private final ClientID clientId3 = new ClientID(3L);

  private InClusterServerEventBuffer buffer;
  private ServerEventBatcher batcher;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    batcher = mock(ServerEventBatcher.class);
    buffer = new InClusterServerEventBuffer(channelManager);
  }

  // @Test
  // public void testShouldCorrectlyRouteEventsAfterRegistration() throws Exception {
  // final ServerEvent event1 = new BasicServerEvent(PUT, 1001, "cache1");
  // final ServerEvent event2 = new BasicServerEvent(EXPIRE, 1002, "cache3");
  // final ServerEvent event3 = new BasicServerEvent(EVICT, 1003, "cache1");
  // final ServerEvent event4 = new BasicServerEvent(PUT, 1004, "cache3");
  //
  // buffer.handleServerEvent(event1);
  // buffer.handleServerEvent(event2);
  // buffer.handleServerEvent(event3);
  // buffer.handleServerEvent(event4);
  //
  // verify(batcher).add(clientId1, event1);
  // verify(batcher).add(clientId2, event2);
  // verify(batcher).add(clientId1, event3);
  // verify(batcher).add(clientId2, event3);
  // verify(batcher, never()).add(any(ClientID.class), eq(event4));
  // verifyNoMoreInteractions(batcher);
  // }
  //
  // @Test
  // public void testShouldCorrectlyRouteEventsAfterUnregisteration() {
  // final ServerEvent event1 = new BasicServerEvent(EVICT, 1001, "cache1");
  // final ServerEvent event2 = new BasicServerEvent(EXPIRE, 1002, "cache1");
  // final ServerEvent event3 = new BasicServerEvent(PUT, 1003, "cache2");
  //
  // buffer.unregister(clientId2, "cache1", EnumSet.of(EVICT, PUT));
  // buffer.unregister(clientId3, "cache2", EnumSet.of(PUT, EXPIRE));
  //
  // buffer.handleServerEvent(event1);
  // buffer.handleServerEvent(event2);
  // buffer.handleServerEvent(event3);
  //
  // verify(batcher).add(clientId1, event1);
  // verify(batcher, never()).add(clientId2, event1);
  // verify(batcher).add(clientId2, event2);
  // verify(batcher).add(clientId1, event3);
  // verify(batcher, never()).add(clientId3, event3);
  // verifyNoMoreInteractions(batcher);
  // }

}
