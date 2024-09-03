/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.test.TCTestCase;

import com.tc.util.concurrent.SetOnceFlag;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelManagerImplTest extends TCTestCase {

  public void testNotifyRemoves() throws Exception {
    SetOnceFlag reconnect = new SetOnceFlag();
    ChannelManagerImpl impl = new ChannelManagerImpl(c->reconnect.isSet(), mock(ServerMessageChannelFactory.class));
    ChannelEvent event = mock(ChannelEvent.class);
    MessageChannel channel = mock(MessageChannel.class);
    when(event.getChannel()).thenReturn(channel);
    when(event.getType()).thenReturn(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT);
    impl.notifyChannelEvent(event);
    verify(channel, Mockito.never()).close();
    reconnect.set();
    impl.notifyChannelEvent(event);
    verify(channel).close();
  }
}
