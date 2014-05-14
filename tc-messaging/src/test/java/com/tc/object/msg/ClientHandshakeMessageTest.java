/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;

public class ClientHandshakeMessageTest {

  @Test
  public void testMessage() throws Exception {

    ClientHandshakeMessageImpl msg = new ClientHandshakeMessageImpl(new SessionID(0), mock(MessageMonitor.class),
                                                                    new TCByteBufferOutputStream(4, 4096, false), null,
                                                                    TCMessageType.CLIENT_HANDSHAKE_MESSAGE);

    ObjectIDSet oids = new BasicObjectIDSet("ImDoingTesting", 12345);
    msg.setObjectIDs(oids);

    ObjectIDSet validations = new BasicObjectIDSet("ImDoingTesting", 1, 2, 100, 200);
    msg.setObjectIDsToValidate(validations);

    msg.dehydrate();

    ClientHandshakeMessageImpl msg2 = new ClientHandshakeMessageImpl(SessionID.NULL_ID, mock(MessageMonitor.class), null,
                                                                     (TCMessageHeader) msg.getHeader(), msg
                                                                         .getPayload());
    msg2.hydrate();

    assertThat(msg.getObjectIDs().size(), is(1));
    assertThat(msg.getObjectIDs(), hasItem(new ObjectID(12345)));

    assertThat(msg.getObjectIDsToValidate(), hasItems(new ObjectID(1), new ObjectID(2), new ObjectID(100), new ObjectID(200)));
    assertThat(msg.getObjectIDsToValidate().size(), is(4));
  }
}
