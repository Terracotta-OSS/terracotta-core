/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.NullTCLogger;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.net.protocol.tcm.TestTCMessage;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class DEV6342Test extends TestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    System.setProperty("com.tc.l1.objectmanager.remote.batchLookupTimePeriod", String.valueOf(500000));
    System.setProperty("com.tc.l1.objectmanager.remote.maxRequestSentImmediately", String.valueOf(0));
  }

  public void testMissingObjectIDsDoesntThrowIllegalException() throws Exception {
    final RemoteObjectManagerImpl manager;
    TestRequestRootMessageFactory rrmf;
    TestRequestManagedObjectMessageFactory rmomf;
    final GroupID groupID;

    final TestChannelIDProvider channelIDProvider = new TestChannelIDProvider();
    channelIDProvider.channelID = new ChannelID(1);
    rmomf = new TestRequestManagedObjectMessageFactory();
    newRmom(rmomf);
    rrmf = new TestRequestRootMessageFactory();
    newRrm(rrmf);

    groupID = new GroupID(0);
    manager = new RemoteObjectManagerImpl(groupID, new NullTCLogger(), rrmf, rmomf, 500, new NullSessionManager());

    final CyclicBarrier barrier = new CyclicBarrier(2);
    final Thread thread = new Thread("Test Thread Saro") {
      @Override
      public void run() {
        System.err.println("Doing a bogus lookup");
        try {
          manager.retrieve(new ObjectID(ObjectID.MAX_ID, groupID.toInt()));
          System.err.println("Didnt throw TCObjectNotFoundException : Not calling barrier()");
        } catch (final TCObjectNotFoundException e) {
          System.err.println("Got TCObjectNotFoundException as expected : " + e);
          try {
            barrier.barrier();
          } catch (final Exception e1) {
            e1.printStackTrace();
          }
        }
      }
    };
    thread.start();
    ThreadUtil.reallySleep(5000);
    final Set missingSet = new HashSet();
    missingSet.add(new ObjectID(ObjectID.MAX_ID, groupID.toInt()));
    manager.objectsNotFoundFor(SessionID.NULL_ID, 1, missingSet, groupID);
    barrier.barrier();
  }

  private TestRequestRootMessage newRrm(TestRequestRootMessageFactory rrmf) {
    final TestRequestRootMessage rv = new TestRequestRootMessage();
    rrmf.message = rv;
    return rv;
  }

  private TestRequestManagedObjectMessage newRmom(TestRequestManagedObjectMessageFactory rmomf) {
    TestRequestManagedObjectMessage rmom;
    rmom = new TestRequestManagedObjectMessage();
    rmomf.message = rmom;
    return rmom;
  }

  private static class TestRequestRootMessageFactory implements RequestRootMessageFactory {
    public final NoExceptionLinkedQueue newMessageQueue = new NoExceptionLinkedQueue();
    public TestRequestRootMessage       message;

    public RequestRootMessage newRequestRootMessage(final NodeID nodeID) {
      this.newMessageQueue.put(this.message);
      return this.message;
    }
  }

  private static class TestRequestRootMessage extends TestTCMessage implements RequestRootMessage {

    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();

    public String getRootName() {
      throw new ImplementMe();
    }

    public void initialize(final String name) {
      return;
    }

    @Override
    public TCMessageType getMessageType() {
      return TCMessageType.REQUEST_ROOT_MESSAGE;
    }

    @Override
    public void send() {
      this.sendQueue.put(new Object());
    }

    public void recycle() {
      return;
    }

  }

  private static class TestRequestManagedObjectMessageFactory implements RequestManagedObjectMessageFactory {

    public final NoExceptionLinkedQueue    newMessageQueue = new NoExceptionLinkedQueue();

    public TestRequestManagedObjectMessage message;

    public RequestManagedObjectMessage newRequestManagedObjectMessage(final NodeID nodeID) {
      this.newMessageQueue.put(this.message);
      return this.message;
    }
  }

  private static class TestRequestManagedObjectMessage implements RequestManagedObjectMessage {

    public final NoExceptionLinkedQueue initializeQueue = new NoExceptionLinkedQueue();
    public final NoExceptionLinkedQueue sendQueue       = new NoExceptionLinkedQueue();

    public ObjectRequestID getRequestID() {
      throw new ImplementMe();
    }

    public ObjectIDSet getRequestedObjectIDs() {
      throw new ImplementMe();
    }

    public ObjectIDSet getRemoved() {
      throw new ImplementMe();
    }

    public void initialize(final ObjectRequestID requestID, final Set<ObjectID> requestedObjectIDs,
                           final int requestDepth, final ObjectIDSet removeObjects) {
      this.initializeQueue.put(new Object[] { requestID, requestedObjectIDs, removeObjects });
    }

    public void send() {
      this.sendQueue.put(new Object());
    }

    public MessageChannel getChannel() {
      throw new ImplementMe();
    }

    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    public int getRequestDepth() {
      return 400;
    }

    public void recycle() {
      return;
    }

    public String getRequestingThreadName() {
      return "TestThreadDummy";
    }

    public LOOKUP_STATE getLookupState() {
      return LOOKUP_STATE.CLIENT;
    }

    public ClientID getClientID() {
      throw new ImplementMe();
    }

    public Object getKey() {
      return new ClientID(1);
    }

  }
}
