/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.session.SessionID;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;


public class ClientHandshakeMessageTest {
  @SuppressWarnings("resource")
  @Test
  public void testMessage() throws Exception {

    ClientHandshakeMessageImpl msg = new ClientHandshakeMessageImpl(new SessionID(0), mock(MessageMonitor.class),
                                                                    new TCByteBufferOutputStream(4, 4096, false), null,
                                                                    TCMessageType.CLIENT_HANDSHAKE_MESSAGE);

    EntityID entity1 = new EntityID("class", "entity 1");
    EntityID entity2 = new EntityID("class", "entity 2");
    EntityID entity3 = new EntityID("class", "entity 3");
    Assert.assertNotEquals(entity1, entity2);
    long entityVersion = 1;
    long instanceID = 0;
    ClientInstanceID clientInstanceID = new ClientInstanceID(instanceID);
    ClientEntityReferenceContext ref1 = new ClientEntityReferenceContext(entity1, entityVersion, clientInstanceID);
    ClientEntityReferenceContext ref2 = new ClientEntityReferenceContext(entity2, entityVersion, clientInstanceID);
    ClientEntityReferenceContext ref3 = new ClientEntityReferenceContext(entity3, entityVersion, clientInstanceID);
    Assert.assertNotEquals(ref1, ref2);
    msg.addReconnectReference(ref1);
    msg.addReconnectReference(ref2);
    msg.dehydrate();

    ClientHandshakeMessageImpl msg2 = new ClientHandshakeMessageImpl(SessionID.NULL_ID, mock(MessageMonitor.class), null,
                                                                     (TCMessageHeader) msg.getHeader(), msg
                                                                         .getPayload());
    msg2.hydrate();
    Collection<ClientEntityReferenceContext> reconnectReferences = msg2.getReconnectReferences();
    Assert.assertTrue(reconnectReferences.contains(ref1));
    Assert.assertTrue(reconnectReferences.contains(ref2));
    Assert.assertFalse(reconnectReferences.contains(ref3));
  }
}
