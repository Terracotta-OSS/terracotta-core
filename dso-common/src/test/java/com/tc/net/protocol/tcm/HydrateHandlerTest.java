/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.test.TCTestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HydrateHandlerTest extends TCTestCase {

  public void testHydrateException() throws Exception {
    HydrateHandler handler = new HydrateHandler();

    Sink sink = mock(Sink.class);
    MessageChannel channel = mock(MessageChannel.class);
    TCMessage message = when(mock(TCMessage.class).getChannel()).thenReturn(channel).getMock();
    doThrow(new RuntimeException("bummer")).when(message).hydrate();

    HydrateContext context = new HydrateContext(message, sink);

    handler.handleEvent(context);
    verify(channel).close();
    verify(message).hydrate();
    verify(sink, never()).add(any(EventContext.class));
  }
}
