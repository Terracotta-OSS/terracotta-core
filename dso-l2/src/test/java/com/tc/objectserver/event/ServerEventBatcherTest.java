package com.tc.objectserver.event;

import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.net.DSOChannelManager;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.Runners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventBatcherTest {

  @Test
  public void testMustPartitionByClient() throws Exception {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    final ServerEventBatcher batcher = new ServerEventBatcher(channelManager, Runners.newSingleThreadScheduledTaskRunner());
    final List<ServerEventBatcher.ClientEnvelope> envelopes = new ArrayList<ServerEventBatcher.ClientEnvelope>(3);

    final ClientID clientId1 = new ClientID(1L);
    final ClientID clientId2 = new ClientID(2L);
    envelopes.add(new ServerEventBatcher.ClientEnvelope(clientId1, new BasicServerEvent(ServerEventType.PUT, 1L, "cache1")));
    envelopes.add(new ServerEventBatcher.ClientEnvelope(clientId1, new BasicServerEvent(ServerEventType.EVICT, 2L, "cache1")));
    envelopes.add(new ServerEventBatcher.ClientEnvelope(clientId2, new BasicServerEvent(ServerEventType.EXPIRE, 3L, "cache2")));

    final Map<ClientID, List<ServerEvent>> groups = batcher.partition(envelopes);
    assertEquals(2, groups.size());
    assertEquals(2, groups.get(clientId1).size());
    assertEquals(1, groups.get(clientId2).size());
    assertEquals(1L, groups.get(clientId1).get(0).getKey());
    assertEquals(2L, groups.get(clientId1).get(1).getKey());
    assertEquals(3L, groups.get(clientId2).get(0).getKey());
  }

  @Test
  public void testMustDrainOnAddIfQueueIsFull() {
    //TODO
  }

  @Test
  public void testMustDrainPeriodically() {
    //TODO
  }

}
