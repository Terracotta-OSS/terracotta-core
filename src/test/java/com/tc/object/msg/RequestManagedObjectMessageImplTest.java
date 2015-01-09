/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import java.util.HashSet;
import java.util.Set;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestContext;
import com.tc.object.ObjectRequestID;
import com.tc.object.session.SessionID;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RequestManagedObjectMessageImplTest {

  @Test
  public void testBasics() throws Exception {
    MessageChannel channel = mock(MessageChannel.class);
    TCMessageType type = TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE;

    TestObjectRequestContext ctxt = new TestObjectRequestContext();
    long[] removed = new long[97];
    for (int i = 0; i < removed.length; i++) {
      removed[i] = i;
    }
    ObjectIDSet removedIDs = new BasicObjectIDSet("ImDoingTesting", removed);

    RequestManagedObjectMessageImpl msg = new RequestManagedObjectMessageImpl(new SessionID(0),
                                                                              mock(MessageMonitor.class),
                                                                              new TCByteBufferOutputStream(4, 4096,
                                                                                                           false),
                                                                              channel, type);
    ObjectIDSet oids = new BasicObjectIDSet("ImDoingTesting", 1);
    msg.initialize(ctxt.getRequestID(), oids, removedIDs);

    msg.dehydrate();

    RequestManagedObjectMessageImpl msg2 = new RequestManagedObjectMessageImpl(SessionID.NULL_ID,
                                                                               mock(MessageMonitor.class), channel,
                                                                               (TCMessageHeader) msg.getHeader(), msg
                                                                                   .getPayload());
    msg2.hydrate();

    Set ids = new HashSet();
    ids.add(new ObjectID(1));

    checkMessageValues(ctxt, removedIDs, ids, msg2);
  }

  private void checkMessageValues(TestObjectRequestContext ctxt, Set removedIDs, Set ids,
                                  RequestManagedObjectMessageImpl msg) {
    assertEquals(ids, new HashSet(msg.getRequestedObjectIDs()));
    assertEquals(ctxt.getRequestID(), msg.getRequestID());
    assertEquals(removedIDs, msg.getRemoved());
  }

  private static class TestObjectRequestContext implements ObjectRequestContext {

    @Override
    public ObjectRequestID getRequestID() {
      return new ObjectRequestID(1);
    }

    @Override
    public ClientID getClientID() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ObjectIDSet getRequestedObjectIDs() {
      throw new UnsupportedOperationException();
    }
  }
}
