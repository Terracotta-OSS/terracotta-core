/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.handler;

import org.junit.Test;

import com.tc.management.ManagementEventListener;
import com.tc.management.TCManagementEvent;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.management.ResponseHolder;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ServerManagementHandlerTest {

  @Test
  public void testArrivingL1EventsAreForwardedToListeners() throws Exception {
    final AtomicBoolean listenerCalled = new AtomicBoolean(false);
    InvokeRegisteredServiceResponseMessage event = mock(InvokeRegisteredServiceResponseMessage.class);
    when(event.getSourceNodeID()).thenReturn(new ClientID(123L));
    MessageChannel channel = mock(MessageChannel.class);
    when(event.getChannel()).thenReturn(channel);
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress(456));
    final TCManagementEvent tcManagementEvent = new TCManagementEvent("this is my test response", "test.type");
    ResponseHolder responseHolder = new ResponseHolder(tcManagementEvent);
    when(event.getResponseHolder()).thenReturn(responseHolder);

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler();
    serverManagementHandler.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return ServerManagementHandlerTest.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent e, Map<String, Object> context) {
        listenerCalled.set(true);
        assertThat(e.getType(), equalTo(tcManagementEvent.getType()));
        assertThat(e.getPayload(), equalTo(tcManagementEvent.getPayload()));
        assertThat((String)context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME), equalTo("123"));
        assertThat((String)context.get(ManagementEventListener.CONTEXT_SOURCE_JMX_ID), endsWith("_456"));
      }
    });

    serverManagementHandler.handleEvent(event);
    assertThat("Expected listener to be called", listenerCalled.get(), is(true));
  }

  @Test
  public void testFireEventCallsListeners() throws Exception {
    final AtomicBoolean listenerCalled = new AtomicBoolean(false);
    final TCManagementEvent tcManagementEvent = new TCManagementEvent("this is my test response", "test.type");
    final Map<String, Object> context = new HashMap<String, Object>();

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler();
    serverManagementHandler.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return ServerManagementHandlerTest.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> ctx) {
        listenerCalled.set(true);
        assertThat(event.getType(), equalTo(tcManagementEvent.getType()));
        assertThat(event.getPayload(), equalTo(tcManagementEvent.getPayload()));
        assertThat(ctx, equalTo(context));
      }
    });

    serverManagementHandler.fireEvent(tcManagementEvent, context);
    assertThat("Expected listener to be called", listenerCalled.get(), is(true));
  }

  @Test
  public void testUnfiredEventsCallListeners() throws Exception {
    final AtomicInteger listenerCalledCount = new AtomicInteger(0);
    final TCManagementEvent tcManagementEvent = new TCManagementEvent("this is my test response", "test.type");
    final Map<String, Object> context = new HashMap<String, Object>();

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler();
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);

    serverManagementHandler.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return ServerManagementHandlerTest.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> ctx) {
        listenerCalledCount.incrementAndGet();
        assertThat(event.getType(), equalTo(tcManagementEvent.getType()));
        assertThat(event.getPayload(), equalTo(tcManagementEvent.getPayload()));
        assertThat(ctx, equalTo(context));
      }
    });

    assertThat("Expected listener to be called twice", listenerCalledCount.get(), is(5));
  }

  @Test
  public void testUnfiredEventsCallListenersWithinCountLimits() throws Exception {
    final AtomicInteger listenerCalledCount = new AtomicInteger(0);
    final TCManagementEvent tcManagementEvent = new TCManagementEvent("this is my test response", "test.type");
    final Map<String, Object> context = new HashMap<String, Object>();

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler() {
      @Override
      int maxUnfiredEventCount() {
        return 3;
      }
    };
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);

    serverManagementHandler.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return ServerManagementHandlerTest.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> ctx) {
        listenerCalledCount.incrementAndGet();
        assertThat(event.getType(), equalTo(tcManagementEvent.getType()));
        assertThat(event.getPayload(), equalTo(tcManagementEvent.getPayload()));
        assertThat(ctx, equalTo(context));
      }
    });

    assertThat("Expected listener to be called twice", listenerCalledCount.get(), is(3));
  }

  @Test
  public void testUnfiredEventsDoNotCallListenersAfterExpiration() throws Exception {
    final AtomicInteger listenerCalledCount = new AtomicInteger(0);
    final TCManagementEvent tcManagementEvent = new TCManagementEvent("this is my test response", "test.type");
    final Map<String, Object> context = new HashMap<String, Object>();

    ServerManagementHandler serverManagementHandler = new ServerManagementHandler() {
      @Override
      int maxUnfiredEventRetentionMillis() {
        return 0;
      }
    };
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);
    serverManagementHandler.fireEvent(tcManagementEvent, context);

    serverManagementHandler.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return ServerManagementHandlerTest.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> ctx) {
        listenerCalledCount.incrementAndGet();
        assertThat(event.getType(), equalTo(tcManagementEvent.getType()));
        assertThat(event.getPayload(), equalTo(tcManagementEvent.getPayload()));
        assertThat(ctx, equalTo(context));
      }
    });

    assertThat("Expected listener to not be called", listenerCalledCount.get(), is(0));
  }
}
