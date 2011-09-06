/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.invalidation.Invalidations;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.Set;

import junit.framework.TestCase;

public class ClientHandshakeMessageTest extends TestCase {

  public void testMessage() throws Exception {

    ClientHandshakeMessageImpl msg = new ClientHandshakeMessageImpl(new SessionID(0), new NullMessageMonitor(),
                                                                    new TCByteBufferOutputStream(4, 4096, false), null,
                                                                    TCMessageType.CLIENT_HANDSHAKE_MESSAGE);

    msg.getObjectIDs().add(new ObjectID(12345));
    Invalidations invalidations = msg.getObjectIDsToValidate();
    invalidations.add(new ObjectID(22), new ObjectID(1));
    invalidations.add(new ObjectID(22), new ObjectID(2));
    invalidations.add(new ObjectID(22), new ObjectID(100));
    invalidations.add(new ObjectID(22), new ObjectID(200));
    msg.dehydrate();

    ClientHandshakeMessageImpl msg2 = new ClientHandshakeMessageImpl(SessionID.NULL_ID, new NullMessageMonitor(), null,
                                                                     (TCMessageHeader) msg.getHeader(), msg
                                                                         .getPayload());
    msg2.hydrate();
    System.out.println(msg2.getObjectIDs());
    System.out.println(msg2.getObjectIDsToValidate());

    Set objectIDs = msg.getObjectIDs();
    Assert.assertEquals(1, objectIDs.size());
    Assert.assertEquals(new ObjectID(12345), objectIDs.iterator().next());
    Assert.assertEquals(new ObjectID(22), msg.getObjectIDsToValidate().getMapIds().iterator().next());
    Invalidations toValidate = msg.getObjectIDsToValidate();
    Assert.assertEquals(4, toValidate.size());
    ObjectIDSet toValidateObjects = toValidate.getObjectIDSetForMapId(new ObjectID(22));
    Assert.assertEquals(4, toValidateObjects.size());
    Assert.assertTrue(toValidateObjects.contains(new ObjectID(1)));
    Assert.assertTrue(toValidateObjects.contains(new ObjectID(2)));
    Assert.assertTrue(toValidateObjects.contains(new ObjectID(100)));
    Assert.assertTrue(toValidateObjects.contains(new ObjectID(200)));

  }
}
