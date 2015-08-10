/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.entity;

import java.io.IOException;
import com.tc.bytes.TCByteBuffer;
import com.tc.entity.VoltronEntityMessage.Type;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import org.junit.Test;

import static org.mockito.Mockito.mock;


/**
 * A simple test to ensure that the various use cases of this message type all serialize/deserialize correctly.
 */
public class NetworkVoltronEntityMessageImplTest {
  @Test
  public void testSerialization() throws UnknownNameException, IOException {
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    MessageChannel channel = null;
    TCMessageType type = TCMessageType.VOLTRON_ENTITY_MESSAGE;
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096, false);
    NetworkVoltronEntityMessageImpl message = new NetworkVoltronEntityMessageImpl(sessionID, monitor, outputStream, channel, type);
    
    ClientID clientID = new ClientID(1);
    TransactionID transactionID = new TransactionID(2);
    EntityDescriptor entityDescriptor = new EntityDescriptor(EntityID.NULL_ID, ClientInstanceID.NULL_ID, 3);
    Type messageType = VoltronEntityMessage.Type.FETCH_ENTITY;
    boolean requiresReplication = false;
    byte[] extendedData = new byte[1];
    message.setContents(clientID, transactionID, entityDescriptor, messageType, requiresReplication, extendedData);
    message.dehydrate();
    
    TCMessageHeader header = (TCMessageHeader) message.getHeader();
    TCByteBuffer[] payload = message.getPayload();
    outputStream.close();
    NetworkVoltronEntityMessageImpl decodingMessage = new NetworkVoltronEntityMessageImpl(SessionID.NULL_ID, monitor, null, header, payload);
    decodingMessage.hydrate();
  }
}
