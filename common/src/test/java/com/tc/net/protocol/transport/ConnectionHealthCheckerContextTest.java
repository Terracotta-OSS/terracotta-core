/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.protocol.transport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.net.core.TCConnectionManager;
import java.net.InetSocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class ConnectionHealthCheckerContextTest {

  private HealthCheckerProbeMessage msg;
  private HealthCheckerConfig config;
  private TestConnectionHealthCheckerContext context;
  private MessageTransportBase messageTransport;

  @Before
  public void setUp() {
    messageTransport = mock(MessageTransportBase.class);
    msg = mock(HealthCheckerProbeMessage.class);

    when(messageTransport.getRemoteAddress()).thenReturn(new InetSocketAddress(9000));

    config = new HealthCheckerConfigImpl("test-config");
    context = new TestConnectionHealthCheckerContext(
        messageTransport, config, null);
  }

  @Test
  public void testShouldCatchTimeDesyncIfThresholdExceeded() {
    when(msg.isTimeCheck()).thenReturn(true);
    when(msg.getTime()).thenReturn(10000L);

    context.receiveProbe(msg);
    Assert.assertEquals(1, context.getDesyncCount());
  }

  @Test
  public void testShouldNotCatchTimeDesyncIfThresholdNotExceeded() {
    when(msg.isTimeCheck()).thenReturn(true);
    when(msg.getTime()).thenReturn(System.currentTimeMillis() - 10000L);

    context.receiveProbe(msg);
    Assert.assertEquals(0, context.getDesyncCount());
  }

  private static class TestConnectionHealthCheckerContext extends ConnectionHealthCheckerContextImpl {

    private int desyncCount;

    public TestConnectionHealthCheckerContext(MessageTransportBase mtb, HealthCheckerConfig config,
                                              TCConnectionManager connMgr) {
      super(mtb, config, connMgr);
    }

    @Override
    void handleTimeDesync(HealthCheckerProbeMessage message, long diff) {
      desyncCount++;
    }

    public int getDesyncCount() {
      return this.desyncCount;
    }
  }
}
