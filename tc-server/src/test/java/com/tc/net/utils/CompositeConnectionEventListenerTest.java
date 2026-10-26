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
package com.tc.net.utils;

import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for CompositeConnectionEventListener.
 */
public class CompositeConnectionEventListenerTest {

  private final TCConnectionEventListener first = mock(TCConnectionEventListener.class);
  private final TCConnectionEventListener second = mock(TCConnectionEventListener.class);
  private final CompositeConnectionEventListener composite = new CompositeConnectionEventListener(first, second);
  private final TCConnectionEvent event = new TCConnectionEvent(mock(TCConnection.class));

  @Test
  public void testConnectEventReachesAllListenersInOrder() {
    composite.connectEvent(event);

    InOrder order = inOrder(first, second);
    order.verify(first).connectEvent(event);
    order.verify(second).connectEvent(event);
  }

  @Test
  public void testCloseEventReachesAllListenersInOrder() {
    composite.closeEvent(event);

    InOrder order = inOrder(first, second);
    order.verify(first).closeEvent(event);
    order.verify(second).closeEvent(event);
  }

  @Test
  public void testEndOfFileEventReachesAllListenersInOrder() {
    composite.endOfFileEvent(event);

    InOrder order = inOrder(first, second);
    order.verify(first).endOfFileEvent(event);
    order.verify(second).endOfFileEvent(event);
  }

  @Test
  public void testErrorEventReachesAllListenersInOrder() {
    TCConnectionErrorEvent error = new TCConnectionErrorEvent(mock(TCConnection.class), new RuntimeException("boom"), null);

    composite.errorEvent(error);

    InOrder order = inOrder(first, second);
    order.verify(first).errorEvent(error);
    order.verify(second).errorEvent(error);
  }
}
