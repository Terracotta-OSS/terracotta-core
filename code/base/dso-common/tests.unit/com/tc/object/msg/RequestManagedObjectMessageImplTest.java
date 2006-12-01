/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;
import com.tc.test.TCTestCase;

import java.util.HashSet;
import java.util.Set;

public class RequestManagedObjectMessageImplTest extends TCTestCase {

  public void testBasics() throws Exception {
    TestMessageChannel channel = new TestMessageChannel();
    TCMessageType type = TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE;

    TestObjectRequestContext ctxt = new TestObjectRequestContext();
    ObjectID id = new ObjectID(1);
    Set removedIDs = new HashSet();
    for (int i = 3; i < 100; i++) {
      removedIDs.add(new ObjectID(i));
    }

    RequestManagedObjectMessageImpl msg = new RequestManagedObjectMessageImpl(new NullMessageMonitor(),
                                                                              new TCByteBufferOutputStream(4, 4096,
                                                                                                           false),
                                                                              channel, type);
    Set oids = new HashSet();
    oids.add(id);
    msg.initialize(ctxt, oids, removedIDs);

    msg.dehydrate();

    RequestManagedObjectMessageImpl msg2 = new RequestManagedObjectMessageImpl(SessionID.NULL_ID,
                                                                               new NullMessageMonitor(), channel,
                                                                               (TCMessageHeader) msg.getHeader(), msg
                                                                                   .getPayload());
    msg2.hydrate();

    Set ids = new HashSet();
    ids.add(id);

    checkMessageValues(ctxt, removedIDs, ids, msg2);
  }

  private void checkMessageValues(TestObjectRequestContext ctxt, Set removedIDs, Set ids,
                                  RequestManagedObjectMessageImpl msg) {
    assertEquals(ids, new HashSet(msg.getObjectIDs()));
    assertEquals(ctxt.getRequestID(), msg.getRequestID());
    assertEquals(removedIDs, msg.getRemoved());
  }

  private static class TestObjectRequestContext implements ObjectRequestContext {
    
    public ObjectRequestID getRequestID() {
      return new ObjectRequestID(1);
    }

    public ChannelID getChannelID() {
      throw new ImplementMe();
    }

    public Set getObjectIDs() {
      throw new ImplementMe();
    }

    public int getRequestDepth() {
      return 10;
    }
  }
}
