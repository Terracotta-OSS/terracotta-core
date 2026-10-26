/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.stats;

import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NullProtocolAdaptor;
import org.terracotta.event.EventService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for ConnectionEventPublisher.
 */
public class ConnectionEventPublisherTest {

  private TCConnectionManager serverConnMgr;
  private TCConnectionManager clientConnMgr;

  @Before
  public void setUp() {
    serverConnMgr = new TCConnectionManagerImpl("Server", null, 0, new ClearTextSocketEndpointFactory());
    clientConnMgr = new TCConnectionManagerImpl("Client", null, 0, new ClearTextSocketEndpointFactory());
  }

  @After
  public void tearDown() {
    clientConnMgr.shutdown();
    serverConnMgr.shutdown();
  }

  // --- constructor ---

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullEventService() {
    new ConnectionEventPublisher(null);
  }

  // --- events ---

  @Test
  public void testConnectEventFiresConnectionOpen() {
    EventService eventService = mock(EventService.class);
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(eventService);

    publisher.connectEvent(new TCConnectionEvent(mock(TCConnection.class)));

    verify(eventService, times(1)).fireEvent(EventService.Event.CONNECTION_OPEN);
    verifyNoMoreInteractions(eventService);
    assertEquals(1, publisher.getConnectionCount());
  }

  @Test
  public void testCloseEventFiresConnectionClose() {
    EventService eventService = mock(EventService.class);
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(eventService);
    TCConnectionEvent event = new TCConnectionEvent(mock(TCConnection.class));

    publisher.connectEvent(event);
    publisher.closeEvent(event);

    verify(eventService, times(1)).fireEvent(EventService.Event.CONNECTION_OPEN);
    verify(eventService, times(1)).fireEvent(EventService.Event.CONNECTION_CLOSE);
    verifyNoMoreInteractions(eventService);
    assertEquals(0, publisher.getConnectionCount());
  }

  @Test
  public void testEndOfFileAndErrorEventsAreIgnored() {
    EventService eventService = mock(EventService.class);
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(eventService);

    publisher.endOfFileEvent(new TCConnectionEvent(mock(TCConnection.class)));
    publisher.errorEvent(null);

    verifyNoMoreInteractions(eventService);
    assertEquals(0, publisher.getConnectionCount());
  }

  // --- real sockets ---

  @Test
  public void testOutboundConnectionOpenAndClose() throws Exception {
    CountingEventService service = new CountingEventService();
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(service);
    TCConnectionManager managed = new TCConnectionManagerImpl("Managed", publisher, 0, new ClearTextSocketEndpointFactory());
    try {
      TCListener listener = listen(serverConnMgr);

      TCConnection conn = managed.createConnection(new NullProtocolAdaptor());
      // creating a connection that is not connected is not a physical connection
      assertEquals(0, publisher.getConnectionCount());

      conn.connect(listener.getBindSocketAddress(), 5000);
      assertEquals(1, publisher.getConnectionCount());

      conn.asynchClose().get();
      service.awaitClose(1);

      assertEquals(0, publisher.getConnectionCount());
      assertEquals(1, service.opens());
      assertEquals(1, service.closes());
    } finally {
      managed.shutdown();
    }
  }

  @Test
  public void testConnectionThatNeverConnectedIsNotCounted() throws Exception {
    CountingEventService service = new CountingEventService();
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(service);
    TCConnectionManager managed = new TCConnectionManagerImpl("Managed", publisher, 0, new ClearTextSocketEndpointFactory());
    try {
      TCConnection conn = managed.createConnection(new NullProtocolAdaptor());
      conn.asynchClose().get();

      assertEquals(0, publisher.getConnectionCount());
      assertEquals(0, service.opens());
      assertEquals(0, service.closes());
    } finally {
      managed.shutdown();
    }
  }

  @Test
  public void testInboundConnectionOpenAndClose() throws Exception {
    CountingEventService service = new CountingEventService();
    ConnectionEventPublisher publisher = new ConnectionEventPublisher(service);
    TCConnectionManager managed = new TCConnectionManagerImpl("Managed", publisher, 0, new ClearTextSocketEndpointFactory());
    try {
      TCListener listener = listen(managed);

      TCConnection first = clientConnMgr.createConnection(new NullProtocolAdaptor());
      TCConnection second = clientConnMgr.createConnection(new NullProtocolAdaptor());
      first.connect(listener.getBindSocketAddress(), 5000);
      second.connect(listener.getBindSocketAddress(), 5000);
      service.awaitOpen(2);
      assertEquals(2, publisher.getConnectionCount());

      first.asynchClose().get();
      service.awaitClose(1);
      assertEquals(1, publisher.getConnectionCount());

      second.asynchClose().get();
      service.awaitClose(2);
      assertEquals(0, publisher.getConnectionCount());

      assertEquals(2, service.opens());
      assertEquals(2, service.closes());
    } finally {
      managed.shutdown();
    }
  }

  private static TCListener listen(TCConnectionManager manager) throws Exception {
    return manager.createListener(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), NullProtocolAdaptor::new);
  }

  /**
   * Counts the connection events and lets a test wait for a given number of them without any timeout.
   */
  private static class CountingEventService implements EventService {
    private final Object lock = new Object();

    private int opens;
    private int closes;

    @Override
    public void fireEvent(Event event, Map<String, String> properties) {
      synchronized (lock) {
        if (event == Event.CONNECTION_OPEN) {
          opens++;
        } else if (event == Event.CONNECTION_CLOSE) {
          closes++;
        }
        lock.notifyAll();
      }
    }

    int opens() {
      synchronized (lock) {
        return opens;
      }
    }

    int closes() {
      synchronized (lock) {
        return closes;
      }
    }

    void awaitOpen(int expected) throws InterruptedException {
      synchronized (lock) {
        while (opens < expected) {
          lock.wait();
        }
      }
    }

    void awaitClose(int expected) throws InterruptedException {
      synchronized (lock) {
        while (closes < expected) {
          lock.wait();
        }
      }
    }
  }
}
