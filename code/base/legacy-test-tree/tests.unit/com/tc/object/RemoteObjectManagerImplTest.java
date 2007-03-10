/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.logging.NullTCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.NullSessionManager;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class RemoteObjectManagerImplTest extends TCTestCase {

  RemoteObjectManagerImpl                        manager;
  ThreadGroup                                    threadGroup;
  private TestChannelIDProvider                  channelIDProvider;
  private TestRequestRootMessageFactory          rrmf;
  private TestRequestManagedObjectMessageFactory rmomf;
  private RetrieverThreads                       rt;

  protected void setUp() throws Exception {
    super.setUp();
    this.channelIDProvider = new TestChannelIDProvider();
    this.channelIDProvider.channelID = new ChannelID(1);
    this.rmomf = new TestRequestManagedObjectMessageFactory();
    newRmom();
    this.rrmf = new TestRequestRootMessageFactory();
    newRrm();

    this.threadGroup = new ThreadGroup(getClass().getName());
    manager = new RemoteObjectManagerImpl(new NullTCLogger(), channelIDProvider, rrmf, rmomf,
                                          new NullObjectRequestMonitor(), 500, new NullSessionManager());
    rt = new RetrieverThreads(Thread.currentThread().getThreadGroup(), manager);
  }

  public void testRequestOutstandingRequestRootMessages() throws Exception {
    final Map expectedResent = new HashMap();
    final Map expectedNotResent = new HashMap();
    TestRequestRootMessage rrm = newRrm();
    assertNoMessageSent(rrm);
    pauseAndStart();
    manager.requestOutstanding();
    manager.unpause();
    assertNoMessageSent(rrm);

    int count = 100;
    for (int i = 0; i < count; i++) {
      newRrm();
      String rootID = "root" + i;
      rt.startNewRootRetriever(rootID);
      Object tmp = rrmf.newMessageQueue.take();
      assertFalse(tmp == rrm);
      rrm = (TestRequestRootMessage) tmp;
      assertTrue(rrmf.newMessageQueue.isEmpty());
      rrm.sendQueue.take();
      assertTrue(rrm.sendQueue.isEmpty());
      if (i % 2 == 0) {
        expectedResent.put(rootID, rrm);
      } else {
        expectedNotResent.put(rootID, rrm);
      }
    }
    log("rt.getAliveCount() = " + rt.getAliveCount() + " expectedResent.size() = " + expectedResent.size()
        + " expectedNotResent.size() = " + expectedNotResent.size());
    assertEquals(count, rt.getAliveCount());
    // respond to some of the requests
    int objectIDCount = 1;
    for (Iterator i = expectedNotResent.keySet().iterator(); i.hasNext();) {
      String rootID = (String) i.next();
      log("Adding Root = " + rootID);
      manager.addRoot(rootID, new ObjectID(objectIDCount++));
    }
    // the threads waiting for the roots we just added should fall through.
    rt.waitForLowWatermark(count - expectedNotResent.size());
    assertEquals(count - expectedResent.size(), rt.getAliveCount());

    // TEST REQUEST OUTSTANDING
    pauseAndStart();
    manager.requestOutstanding();
    manager.unpause();

    assertFalse(rrmf.newMessageQueue.isEmpty());

    // Check the messages we expect to have been resent
    for (Iterator i = expectedResent.values().iterator(); i.hasNext(); i.next()) {
      rrm = (TestRequestRootMessage) rrmf.newMessageQueue.take();
      assertNotNull(rrm.sendQueue.poll(1));
    }

    for (Iterator i = expectedNotResent.values().iterator(); i.hasNext();) {
      rrm = (TestRequestRootMessage) i.next();
      assertTrue(rrm.sendQueue.isEmpty());
    }

    assertTrue(rrmf.newMessageQueue.isEmpty());

    // respond to the rest of the requests
    for (Iterator i = expectedResent.keySet().iterator(); i.hasNext();) {
      String rootID = (String) i.next();
      log("Adding Root = " + rootID);
      manager.addRoot(rootID, new ObjectID(objectIDCount++));
    }

    // all the threads should now be able to complete.
    rt.waitForLowWatermark(0);

  }

  private static void log(String s) {
    if (false) System.err.println(Thread.currentThread().getName() + " :: " + s);
  }

  private void pauseAndStart() {
    manager.pause();
    // manager.clearCache();
    manager.starting();
  }

  public void testRequestOutstandingRequestManagedObjectMessages() throws Exception {

    final Map expectedResent = new HashMap();
    final Map secondaryResent = new HashMap();
    final Map expectedNotResent = new HashMap();

    TestRequestManagedObjectMessage rmom = newRmom();
    assertNoMessageSent(rmom);
    pauseAndStart();
    manager.requestOutstanding();
    manager.unpause();
    assertNoMessageSent(rmom);

    int count = 50;

    for (int i = 0; i < count; i++) {
      newRmom();
      ObjectID id = new ObjectID(i);
      assertTrue(rmomf.newMessageQueue.isEmpty());
      rt.startNewObjectRetriever(id);
      Object tmp = rmomf.newMessageQueue.take();
      assertTrue(rmomf.newMessageQueue.isEmpty());
      // make sure we aren't mistakenly using the same message all the time
      assertFalse(rmom == tmp);
      rmom = (TestRequestManagedObjectMessage) tmp;
      rmom.sendQueue.take();
      assertEquals(i + 1, rt.getAliveCount());
      if (i % 2 == 0) {
        expectedResent.put(id, rmom);
      } else {
        expectedNotResent.put(id, rmom);
      }
    }

    // request the same objects again
    for (int i = 0; i < count; i++) {
      newRmom();
      ObjectID id = new ObjectID(i);
      assertTrue(rmomf.newMessageQueue.isEmpty());
      rt.startNewObjectRetriever(id);
      assertTrue(rmomf.newMessageQueue.isEmpty());
    }

    assertTrue(rmomf.newMessageQueue.isEmpty());

    // now go through all of the messages we don't expect to be resent and respond to their requests
    for (Iterator i = expectedNotResent.keySet().iterator(); i.hasNext();) {
      newRmom();
      assertTrue(rmomf.newMessageQueue.isEmpty());
      manager.addObject(new TestDNA((ObjectID) i.next()));
      // collect the messages sent for the secondary threads...
      Object tmp = rmomf.newMessageQueue.take();
      assertFalse(rmom == tmp);
      rmom = (TestRequestManagedObjectMessage) tmp;
      rmom.sendQueue.take();
      assertTrue(rmom.sendQueue.isEmpty());
      secondaryResent.put(rmom.objectIDs.iterator().next(), rmom);
    }

    // now tell it to resend outstanding
    pauseAndStart();
    manager.requestOutstanding();
    manager.unpause();

    final Collection c = new LinkedList();
    c.addAll(expectedResent.values());
    c.addAll(secondaryResent.values());
    // now go through all of the messages we DO expect to be resent and make sure that
    // they WERE resent
    for (Iterator i = c.iterator(); i.hasNext(); i.next()) {
      rmom = (TestRequestManagedObjectMessage) rmomf.newMessageQueue.take();
      assertFalse(rmom.sendQueue.isEmpty());
      assertNotNull(rmom.sendQueue.poll(1));
    }

    c.clear();

    // go through all of the messages we DON'T expect to be resent and make sure they WEREN'T resent

    c.addAll(expectedNotResent.values());
    for (Iterator i = c.iterator(); i.hasNext();) {
      rmom = (TestRequestManagedObjectMessage) i.next();
      assertTrue(rmom.sendQueue.isEmpty());
    }
  }

  public void testBasics() throws Exception {

    final ObjectID id1 = new ObjectID(1);
    final ObjectID id2 = new ObjectID(2);
    final ObjectID id200 = new ObjectID(200);
    final ObjectID id201 = new ObjectID(201);
    final Set removed = new HashSet();
    removed.add(id1);
    removed.add(id200);
    removed.add(id201);
    // set up some removed objects.
    for (Iterator i = removed.iterator(); i.hasNext();) {
      this.manager.removed((ObjectID) i.next());
    }

    TestRequestManagedObjectMessage rmom = this.rmomf.message;
    assertNoMessageSent(rmom);

    rt.startNewObjectRetriever(id1);

    waitForMessageSend(rmom);

    assertNoMessageSent(rmom);

    // Check to see that the message was initialized with the expected
    // values
    verifyRmomInit(id1, removed, rmom);

    assertEquals(1, rt.getAliveCount());

    rmom = newRmom();

    // now request the same object id with a different thread.
    rt.startNewObjectRetriever(id1);

    // but, no message should have been sent.
    assertTrue(rmom.sendQueue.isEmpty());

    assertEquals(2, rt.getAliveCount());

    // now request a different object id on a different thread
    rt.startNewObjectRetriever(id2);

    // this thread should send a message with id2, an empty set for the
    // removed
    // ids
    waitForMessageSend(rmom);
    verifyRmomInit(id2, new HashSet(), rmom);

    assertEquals(3, rt.getAliveCount());

    assertNoMessageSent(rmom);
    rmom = newRmom();

    // this should allow the first two threads to fall through
    // XXX: Actually, it doesn't. The way it's implemented, each object
    // request
    // will result in its own message send.
    //
    // This is sub-optimal, but it works so we're not changing it right now,
    // especially since we're going to have to optimize this stuff soon
    // anyway. --Orion 8/24/05

    manager.addObject(new TestDNA(id1));
    rt.waitForLowWatermark(2);

    waitForMessageSend(rmom);
    verifyRmomInit(id1, new HashSet(), rmom);

    rmom = newRmom();

    // the second thread should now create and send a new message
    manager.addObject(new TestDNA(id1));
    rt.waitForLowWatermark(1);

    // now, allow the third thread to fall through
    manager.addObject(new TestDNA(id2));
    rt.waitForLowWatermark(0);

    // no-one should have sent any messages
    assertNoMessageSent(rmom);
  }

  private void assertNoMessageSent(TestRequestManagedObjectMessage rmom) {
    assertTrue(rmomf.newMessageQueue.isEmpty());
    assertTrue(rmom.sendQueue.isEmpty());
  }

  private void assertNoMessageSent(TestRequestRootMessage rrm) {
    assertTrue(rrmf.newMessageQueue.isEmpty());
    assertTrue(rrm.sendQueue.isEmpty());
  }

  private void waitForMessageSend(TestRequestManagedObjectMessage rmom) {
    rmomf.newMessageQueue.take();
    rmom.sendQueue.take();
  }

  /**
   * Verifies that the object request message initialization was done according to the given arguments.
   */
  private void verifyRmomInit(final ObjectID objectID, final Set removed, TestRequestManagedObjectMessage rmom) {
    Object[] initArgs = (Object[]) rmom.initializeQueue.take();
    Set oids = new HashSet();
    oids.add(objectID);
    assertTrue(rmom.initializeQueue.isEmpty());
    ObjectRequestContext ctxt = (ObjectRequestContext) initArgs[0];
    assertEquals(this.channelIDProvider.channelID, ctxt.getChannelID());
    assertEquals(oids, ctxt.getObjectIDs());
    // The object id in the request
    assertEquals(oids, initArgs[1]);
    // The proper set of removed object ids
    assertEquals(removed, initArgs[2]);
  }

  private TestRequestRootMessage newRrm() {
    TestRequestRootMessage rv = new TestRequestRootMessage();
    this.rrmf.message = rv;
    return rv;
  }

  private TestRequestManagedObjectMessage newRmom() {
    TestRequestManagedObjectMessage rmom;
    rmom = new TestRequestManagedObjectMessage();
    this.rmomf.message = rmom;
    return rmom;
  }

  private static class RetrieverThreads {
    private int                       threadCount;

    private final RemoteObjectManager manager;

    private final Set                 inProgress = new HashSet();

    private final ThreadGroup         tg;

    public RetrieverThreads(ThreadGroup tg, RemoteObjectManager manager) {
      this.manager = manager;
      this.tg = tg;
    }

    public int getAliveCount() {
      synchronized (inProgress) {
        return inProgress.size();
      }
    }

    public void waitForLowWatermark(int max) throws InterruptedException {
      if (getAliveCount() <= max) return;
      synchronized (inProgress) {
        while (getAliveCount() > max) {
          inProgress.wait();
        }
      }
    }

    public Thread startNewRootRetriever(final String rootID) {
      Thread t = new Thread(tg, new Runnable() {

        public void run() {
          log("Starting .. " + rootID);
          manager.retrieveRootID(rootID);
          log("Retrieved  rootID.. " + rootID);
          synchronized (inProgress) {
            if (!inProgress.remove(Thread.currentThread())) throw new RuntimeException("Thread not removed!");
            log("Removed from  inProgress .. size =  " + inProgress.size());
            inProgress.notifyAll();
          }
        }
      }, "Root retriever thread " + threadCount++);
      synchronized (inProgress) {
        inProgress.add(t);
        log("Added : inProgress size = " + inProgress.size());
      }
      t.start();
      return t;
    }

    public Thread startNewObjectRetriever(final ObjectID id) {
      Thread t = new Thread(tg, new Runnable() {

        public void run() {
          manager.retrieve(id);
          synchronized (inProgress) {
            if (!inProgress.remove(Thread.currentThread())) throw new RuntimeException("Thread not removed!");
            inProgress.notifyAll();
          }
        }
      }, "Object retriever thread " + threadCount++);
      synchronized (inProgress) {
        inProgress.add(t);
      }
      t.start();
      return t;
    }
  }

  private static class TestRequestRootMessageFactory implements RequestRootMessageFactory {
    public final NoExceptionLinkedQueue newMessageQueue = new NoExceptionLinkedQueue();
    public TestRequestRootMessage       message;

    public RequestRootMessage newRequestRootMessage() {
      newMessageQueue.put(message);
      return this.message;
    }

  }

  private static class TestRequestRootMessage implements RequestRootMessage {

    public final NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();

    public String getRootName() {
      throw new ImplementMe();
    }

    public void initialize(String name) {
      return;
    }

    public void send() {
      sendQueue.put(new Object());
    }

    public ChannelID getChannelID() {
      throw new ImplementMe();
    }

    public void recycle() {
      return;
    }

  }

  private static class TestRequestManagedObjectMessageFactory implements RequestManagedObjectMessageFactory {

    public final NoExceptionLinkedQueue    newMessageQueue = new NoExceptionLinkedQueue();

    public TestRequestManagedObjectMessage message;

    public RequestManagedObjectMessage newRequestManagedObjectMessage() {
      newMessageQueue.put(message);
      return message;
    }

  }

  private static class TestRequestManagedObjectMessage implements RequestManagedObjectMessage {

    public final NoExceptionLinkedQueue initializeQueue = new NoExceptionLinkedQueue();
    public final NoExceptionLinkedQueue sendQueue       = new NoExceptionLinkedQueue();
    public Set                          objectIDs;

    public ObjectRequestID getRequestID() {
      throw new ImplementMe();
    }

    public Set getObjectIDs() {
      throw new ImplementMe();
    }

    public Set getRemoved() {
      throw new ImplementMe();
    }

    public void initialize(ObjectRequestContext ctxt, Set oids, Set removedIDs) {
      this.objectIDs = oids;
      this.initializeQueue.put(new Object[] { ctxt, oids, removedIDs });
    }

    public void send() {
      sendQueue.put(new Object());
    }

    public MessageChannel getChannel() {
      throw new ImplementMe();
    }

    public ChannelID getChannelID() {
      throw new ImplementMe();
    }

    public int getRequestDepth() {
      return 400;
    }

    public void recycle() {
      return;
    }

  }

}
