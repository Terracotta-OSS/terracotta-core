/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SystemTimeDifferenceOperatorEventTest extends TCTestCase {
  private final TerracottaOperatorEventHistoryProvider opEventHistoryProvider = new MockTerracottaOperatorEventHistoryProvider();

  @Override
  protected void setUp() throws Exception {
    TerracottaOperatorEventLogger opEventLogger = TerracottaOperatorEventLogging.getEventLogger();
    opEventLogger.registerForHistory(this.opEventHistoryProvider);
  }

  public void testSystemTimeDifferenceOperatorEvent() {
    ClientHandshakeHandler handshakeHandler = new EnterpriseClientHandshakeHandler("test-server");
    ServerConfigurationContext serverConfigurationContext = when(mock(ServerConfigurationContext.class).getClientHandshakeManager())
        .thenReturn(new TestServerClientHandshakeManager()).getMock();
    handshakeHandler.initialize(serverConfigurationContext);
    ClientHandshakeMessage handshakeMessage = handshakeMessage();
    handshakeHandler.handleEvent(handshakeMessage);
    System.out.println("XXXXX: " + this.opEventHistoryProvider.getOperatorEvents());
    TerracottaOperatorEvent timeDifferenceEvent = TerracottaOperatorEventFactory
        .createSystemTimeDifferentEvent(ClientID.NULL_ID, "xyz", "ABC", 1235);

    boolean found = false;
    List<TerracottaOperatorEvent> operatorEvents = this.opEventHistoryProvider.getOperatorEvents();
    for (TerracottaOperatorEvent operatorEvent : operatorEvents) {
      if (timeDifferenceEvent.getEventLevel() != operatorEvent.getEventLevel()) continue;
      if (timeDifferenceEvent.getEventSubsystem() != operatorEvent.getEventSubsystem()) continue;
      if (!timeDifferenceEvent.getCollapseString().equals(operatorEvent.getCollapseString())) continue;
      found = true;
      break;
    }

    Assert.assertTrue(found);
  }

  private ClientHandshakeMessage handshakeMessage() {
    MessageChannel channel = mock(MessageChannel.class);
    ClientHandshakeMessage clientHandshakeMessage = when(mock(ClientHandshakeMessage.class).getChannel()).thenReturn(channel).getMock();
    when(clientHandshakeMessage.getLocalTimeMills()).thenReturn(0L);
    when(clientHandshakeMessage.enterpriseClient()).thenReturn(true);
    return clientHandshakeMessage;
  }

  private static class MockTerracottaOperatorEventHistoryProvider implements TerracottaOperatorEventHistoryProvider {
    private final List<TerracottaOperatorEvent> operatorEvents = new ArrayList<>();

    @Override
    public List<TerracottaOperatorEvent> getOperatorEvents() {
      return this.operatorEvents;
    }

    @Override
    public List<TerracottaOperatorEvent> getOperatorEvents(long sinceTimestamp) {
      List<TerracottaOperatorEvent> eventList = new ArrayList<>();
      Date sinceDate = new Date(sinceTimestamp);
      for(TerracottaOperatorEvent event : operatorEvents) {
        if(event.getEventTime().after(sinceDate)) {
          eventList.add(event);
        }
      }
      return eventList;
    }

    @Override
    public void push(TerracottaOperatorEvent event) {
      this.operatorEvents.add(event);
    }

    @Override
    public Map<String, Integer> getUnreadCounts() {
      return null;
    }

    @Override
    public boolean markOperatorEvent(TerracottaOperatorEvent operatorEvent, boolean read) {
      return true;
    }

  }

  private static class TestServerClientHandshakeManager extends ServerClientHandshakeManager {

    public TestServerClientHandshakeManager() {
      super(null, null, null, null, null, null, 0, false, null);
    }

    @Override
    public void notifyClientConnect(ClientHandshakeMessage handshake) {
      //
    }

  }
}
