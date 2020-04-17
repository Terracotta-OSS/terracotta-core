/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.test.TCTestCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HydrateHandlerTest extends TCTestCase {

  public void testHydrateException() throws Exception {
    HydrateHandler handler = new HydrateHandler();

    Sink<TCMessage> sink = mock(Sink.class);
    MessageChannel channel = mock(MessageChannel.class);
    TCMessage msg = mock(TCMessage.class);
    when(msg.getChannel()).thenReturn(channel);
    doThrow(new RuntimeException("bummer")).when(msg).hydrate();

    HydrateContext context = new HydrateContext(msg, sink);

    handler.handleEvent(context);
    verify(channel).close();
    verify(msg).hydrate();
    verify(sink, never()).addToSink(any(TCMessage.class));
  }
}
