/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
