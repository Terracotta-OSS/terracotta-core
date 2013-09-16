package com.tc.object.handler;

import org.junit.Test;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventDeliveryHandlerTest {

  @Test
  public void testMustForwardEventToManager() {
    final ServerEvent event = new BasicServerEvent(ServerEventType.PUT, "key1", "test-cache");
    final NodeID remoteNode = new GroupID(0);
    final ServerEventListenerManager manager = mock(ServerEventListenerManager.class);

    final ServerEventDeliveryHandler handler = new ServerEventDeliveryHandler(manager);
    handler.handleEvent(new ServerEventDeliveryContext(event, remoteNode));

    verify(manager).dispatch(event, remoteNode);
    verifyNoMoreInteractions(manager);
  }
}
