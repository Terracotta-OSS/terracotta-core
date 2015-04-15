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
package com.tc.net.protocol.transport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnectionManager;

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

    when(messageTransport.getRemoteAddress()).thenReturn(new TCSocketAddress(9000));

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

    public TestConnectionHealthCheckerContext(final MessageTransportBase mtb, final HealthCheckerConfig config,
                                              final TCConnectionManager connMgr) {
      super(mtb, config, connMgr);
    }

    @Override
    void handleTimeDesync(final HealthCheckerProbeMessage message, final long diff) {
      desyncCount++;
    }

    public int getDesyncCount() {
      return this.desyncCount;
    }
  }
}
