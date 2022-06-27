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
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.test.TCTestCase;

import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelManagerImplTest extends TCTestCase {

  public void testNotifyRemoves() throws Exception {
    AtomicBoolean reconnect = new AtomicBoolean();
    ChannelManagerImpl impl = new ChannelManagerImpl(c->reconnect.get(), mock(ServerMessageChannelFactory.class));
    ChannelEvent event = mock(ChannelEvent.class);
    MessageChannel channel = mock(MessageChannel.class);
    when(event.getChannel()).thenReturn(channel);
    when(event.getType()).thenReturn(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT);
    impl.notifyChannelEvent(event);
    verify(channel, Mockito.never()).close();
    reconnect.set(true);
    impl.notifyChannelEvent(event);
    verify(channel).close();
  }
}
