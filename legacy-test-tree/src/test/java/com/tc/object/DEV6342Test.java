/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortedOperationException;
import com.tc.abortable.NullAbortableOperationManager;
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
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

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
    manager = new RemoteObjectManagerImpl(groupID, new NullTCLogger(), rrmf, rmomf, 500, new NullSessionManager(),
                                          new NullAbortableOperationManager(), Runners.newSingleThreadScheduledTaskRunner());

    final CyclicBarrier barrier = new CyclicBarrier(2);
    final Thread thread = new Thread("Test Thread Saro") {
      @Override
      public void run() {
        System.err.println("Doing a bogus lookup");
        try {
          try {
            manager.retrieve(new ObjectID(ObjectID.MAX_ID, groupID.toInt()));
          } catch (AbortedOperationException e) {
            throw new AssertionError(e);
          }
          System.err.println("Didnt throw TCObjectNotFoundException : Not calling barrier()");
        } catch (final TCObjectNotFoundException e) {
          System.err.println("Got TCObjectNotFoundException as expected : " + e);
          try {
            barrier.await();
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
    barrier.await();
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

    @Override
    public RequestRootMessage newRequestRootMessage(final NodeID nodeID) {
      this.newMessageQueue.put(this.message);
      return this.message;
    }
  }

  private static class TestRequestRootMessage extends TestTCMessage implements RequestRootMessage {

    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();

    @Override
    public String getRootName() {
      throw new ImplementMe();
    }

    @Override
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

    @Override
    public void recycle() {
      return;
    }

  }

  private static class TestRequestManagedObjectMessageFactory implements RequestManagedObjectMessageFactory {

    public final NoExceptionLinkedQueue    newMessageQueue = new NoExceptionLinkedQueue();

    public TestRequestManagedObjectMessage message;

    @Override
    public RequestManagedObjectMessage newRequestManagedObjectMessage(final NodeID nodeID) {
      this.newMessageQueue.put(this.message);
      return this.message;
    }
  }

  private static class TestRequestManagedObjectMessage implements RequestManagedObjectMessage {

    public final NoExceptionLinkedQueue initializeQueue = new NoExceptionLinkedQueue();
    public final NoExceptionLinkedQueue sendQueue       = new NoExceptionLinkedQueue();

    @Override
    public ObjectRequestID getRequestID() {
      throw new ImplementMe();
    }

    @Override
    public ObjectIDSet getRequestedObjectIDs() {
      throw new ImplementMe();
    }

    @Override
    public ObjectIDSet getRemoved() {
      throw new ImplementMe();
    }

    @Override
    public void initialize(final ObjectRequestID requestID, final ObjectIDSet requestedObjectIDs,
                           final int requestDepth, final ObjectIDSet removeObjects) {
      this.initializeQueue.put(new Object[] { requestID, requestedObjectIDs, removeObjects });
    }

    @Override
    public void send() {
      this.sendQueue.put(new Object());
    }

    @Override
    public MessageChannel getChannel() {
      throw new ImplementMe();
    }

    @Override
    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    @Override
    public int getRequestDepth() {
      return 400;
    }

    @Override
    public void recycle() {
      return;
    }

    @Override
    public String getRequestingThreadName() {
      return "TestThreadDummy";
    }

    @Override
    public LOOKUP_STATE getLookupState() {
      return LOOKUP_STATE.CLIENT;
    }

    @Override
    public ClientID getClientID() {
      throw new ImplementMe();
    }

    @Override
    public Object getKey() {
      return new ClientID(1);
    }

  }
}
