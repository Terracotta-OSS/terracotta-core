/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.handler;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.ObjectID;
import com.tc.object.msg.ServerMapEvictionBroadcastMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.session.SessionID;
import com.tc.objectserver.context.ServerMapEvictionBroadcastContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class BroadcastEvictionsTest {

  private ClientStateManager clientStateManagerMock;
  private DSOChannelManager channelManagerMock;
  private ServerConfigurationContext configurationContextMock;
  private MessageChannel channelMock;

  @Before
  public void setUp() {
    clientStateManagerMock = mock(ClientStateManager.class);
    channelManagerMock = mock(DSOChannelManager.class);
    configurationContextMock = mock(ServerConfigurationContext.class);
    channelMock = mock(MessageChannel.class);
  }

  @Test
  public void testBroadcastEvictionsOnlyIfEventListenerRegistered() {
    // given
    final Set<Integer> evictedKeys = new HashSet<Integer>();
    evictedKeys.add(1);
    final ServerMapEvictionBroadcastContext context1 =
        new ServerMapEvictionBroadcastContext(new ObjectID(1L), evictedKeys, true);
    final ServerMapEvictionBroadcastContext context2 =
        new ServerMapEvictionBroadcastContext(new ObjectID(2L), evictedKeys, false);
    final ServerMapEvictionBroadcastHandler handler = new ServerMapEvictionBroadcastHandler();
    final MockServerMapEvictionBroadcastMessage message = new MockServerMapEvictionBroadcastMessage();

    // when
    when(configurationContextMock.getChannelManager()).thenReturn(channelManagerMock);
    when(configurationContextMock.getClientStateManager()).thenReturn(clientStateManagerMock);
    when(channelManagerMock.getActiveChannels()).thenReturn(new MessageChannel[] { channelMock });
    when(channelMock.isClosed()).thenReturn(false);
    when(clientStateManagerMock.hasReference(any(NodeID.class), any(ObjectID.class))).thenReturn(true);
    when(channelMock.createMessage(any(TCMessageType.class))).thenReturn(message);

    // then
    //assert
    handler.initialize(configurationContextMock);
    handler.handleEvent(context1);
    assertEquals(1, message.getSendCount());
    handler.handleEvent(context2);
    // no messages sent from last call
    assertEquals(1, message.getSendCount());
  }


  private static class MockServerMapEvictionBroadcastMessage implements ServerMapEvictionBroadcastMessage {

    private int sendCount;

    @Override
    public void send() {
      sendCount++;
    }

    public int getSendCount() {
      return sendCount;
    }

    @Override
    public void initializeEvictionBroadcastMessage(final ObjectID mapID, final Set evictedKeys, final int clientIndex) {
    }

    @Override
    public ObjectID getMapID() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Set getEvictedKeys() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public int getClientIndex() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public TCMessageType getMessageType() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public void hydrate() throws IOException, UnknownNameException {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public void dehydrate() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public MessageChannel getChannel() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public NodeID getSourceNodeID() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public NodeID getDestinationNodeID() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public SessionID getLocalSessionID() {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public int getTotalLength() {
      throw new UnsupportedOperationException("Implement me!");
    }
  }

}
