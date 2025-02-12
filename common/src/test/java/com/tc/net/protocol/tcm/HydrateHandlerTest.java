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

    Sink<TCAction> sink = mock(Sink.class);
    MessageChannel channel = mock(MessageChannel.class);
    TCAction msg = mock(TCAction.class);
    when(msg.getChannel()).thenReturn(channel);
    doThrow(new RuntimeException("bummer")).when(msg).hydrate();

    HydrateContext context = new HydrateContext(msg, sink);

    handler.handleEvent(context);
    verify(channel).close();
    verify(msg).hydrate();
    verify(sink, never()).addToSink(any(TCAction.class));
  }
}
