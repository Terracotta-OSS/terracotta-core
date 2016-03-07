package com.tc.object.msg;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
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

/**
 * @author vmad
 */
public class ClientHandshakeMessageImplTest {

    @Test
    public void testGetResendMessages() throws Exception {

        ClientHandshakeMessageImpl chm = new ClientHandshakeMessageImpl(mock(SessionID.class), mock(MessageMonitor.class),
                new TCByteBufferOutputStream(), mock(MessageChannel.class), TCMessageType.getInstance(TCMessageType.TYPE_CLIENT_HANDSHAKE_MESSAGE));
        ResendVoltronEntityMessage msg1 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(1),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.FETCH_ENTITY, false, new byte[0], mock(TransactionID.class));
        ResendVoltronEntityMessage msg2 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(10),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.CREATE_ENTITY, false, new byte[0], mock(TransactionID.class));
        ResendVoltronEntityMessage msg3 = new ResendVoltronEntityMessage(mock(ClientID.class), new TransactionID(40),
                mock(EntityDescriptor.class), VoltronEntityMessage.Type.DESTROY_ENTITY, false, new byte[0], mock(TransactionID.class));

        chm.addResendMessage(msg3);
        chm.addResendMessage(msg2);
        chm.addResendMessage(msg1);

        Assert.assertThat("resend transactions are out of order", chm.getResendMessages().toArray(new ResendVoltronEntityMessage[3]),
                equalTo(new ResendVoltronEntityMessage[] { msg1, msg2, msg3 }));
    }

}