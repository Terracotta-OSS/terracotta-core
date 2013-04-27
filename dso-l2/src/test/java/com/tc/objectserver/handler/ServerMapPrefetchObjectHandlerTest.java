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
public class ServerMapPrefetchObjectHandlerTest {

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
  }

}
