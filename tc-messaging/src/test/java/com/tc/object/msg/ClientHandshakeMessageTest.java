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
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.session.SessionID;
import java.net.InetSocketAddress;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ClientHandshakeMessageTest {
  @Test
  public void testMessage() throws Exception {
    MessageChannel channel = mock(MessageChannel.class);
    InetSocketAddress socket = new InetSocketAddress(65432);
    when(channel.getLocalAddress()).thenReturn(socket);

    ClientHandshakeMessageImpl msg = new ClientHandshakeMessageImpl(new SessionID(0), mock(MessageMonitor.class),
                                                                    new TCByteBufferOutputStream(4, 4096), channel,
                                                                    TCMessageType.CLIENT_HANDSHAKE_MESSAGE);

    EntityID entity1 = new EntityID("class", "entity 1");
    EntityID entity2 = new EntityID("class", "entity 2");
    EntityID entity3 = new EntityID("class", "entity 3");
    Assert.assertNotEquals(entity1, entity2);
    long entityVersion = 1;
    long instanceID = 0;
    byte[] extendedReconnectData1 = {};
    byte[] extendedReconnectData2 = {1, 2, 3};
    byte[] extendedReconnectData3 = {3, 4, 5, 6};
    ClientEntityReferenceContext ref1 = new ClientEntityReferenceContext(entity1, entityVersion, new ClientInstanceID(instanceID++), extendedReconnectData1);
    ClientEntityReferenceContext ref2 = new ClientEntityReferenceContext(entity2, entityVersion, new ClientInstanceID(instanceID++), extendedReconnectData2);
    ClientEntityReferenceContext ref3 = new ClientEntityReferenceContext(entity3, entityVersion, new ClientInstanceID(instanceID++), extendedReconnectData3);
    Assert.assertNotEquals(ref1, ref2);
    msg.addReconnectReference(ref1);
    msg.addReconnectReference(ref2);
    TCNetworkMessage nmsg = msg.convertToNetworkMessage();

    ClientHandshakeMessageImpl msg2 = new ClientHandshakeMessageImpl(SessionID.NULL_ID, mock(MessageMonitor.class), channel,
                                                                     (TCMessageHeader)nmsg.getHeader(), new TCByteBufferInputStream(nmsg.getPayload()));
    msg2.hydrate();
    Collection<ClientEntityReferenceContext> reconnectReferences = msg2.getReconnectReferences();
    Assert.assertTrue(reconnectReferences.contains(ref1));
    Assert.assertTrue(reconnectReferences.contains(ref2));
    Assert.assertFalse(reconnectReferences.contains(ref3));
  }
}
