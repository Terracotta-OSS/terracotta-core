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

import com.tc.bytes.TCByteBufferFactory;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author vmad
 */
public class ClientHandshakeMessageImplTest {

    @Test
    public void testGetResendMessages() throws Exception {
        MessageChannel channel = mock(MessageChannel.class);
        TCSocketAddress socket = new TCSocketAddress(65432);
        when(channel.getLocalAddress()).thenReturn(socket);
    
        ClientHandshakeMessageImpl chm = new ClientHandshakeMessageImpl(mock(SessionID.class), mock(MessageMonitor.class),
                new TCByteBufferOutputStream(), channel, TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
        ResendVoltronEntityMessage msg1 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(1),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.FETCH_ENTITY, false, TCByteBufferFactory.getInstance(false, 0), mock(TransactionID.class));
        ResendVoltronEntityMessage msg2 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(10),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.CREATE_ENTITY, false, TCByteBufferFactory.getInstance(false, 0), mock(TransactionID.class));
        ResendVoltronEntityMessage msg3 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(40),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.DESTROY_ENTITY, false, TCByteBufferFactory.getInstance(false, 0), mock(TransactionID.class));

        chm.addResendMessage(msg3);
        chm.addResendMessage(msg2);
        chm.addResendMessage(msg1);

        Assert.assertThat("resend transactions are out of order", chm.getResendMessages().toArray(new ResendVoltronEntityMessage[3]),
                equalTo(new ResendVoltronEntityMessage[] { msg1, msg2, msg3 }));
    }

}