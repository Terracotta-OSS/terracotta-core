/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;

import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public class SyncObjectIdSetImplTest extends TestCase {

  private SyncObjectIdSetImpl set;
  private int                 threadCnt;
  private CountDownLatch      preMon;
  private CountDownLatch      postMon;
  private ObjectID            id1;
  private ObjectID            id2;

  @Override
  protected void setUp() throws Exception {
    id1 = new ObjectID(1);
    id2 = new ObjectID(2);
    set = new SyncObjectIdSetImpl();
    threadCnt = 5;
    preMon = new CountDownLatch(threadCnt);
    postMon = new CountDownLatch(threadCnt);
  }

  public void testRemove() throws Throwable {
    set.add(id1);
    set.add(id2);
    assertEquals(2, set.size());

    // test non-blocking...
    set.remove(id1);
    assertEquals(1, set.size());

    // test blocking...
    checkBlocking(new RemoveCaller(id2));
    assertEquals(0, set.size());
  }

  public void testNonBlockingContains() throws Throwable {
    final ObjectID id3 = new ObjectID(3);
    set.add(id1);
    set.add(id2);
    assertEquals(2, set.size());

    // test non-blocking...
    assertTrue(set.contains(id1));
    assertTrue(set.contains(id2));
    assertFalse(set.contains(id3));

    set.startPopulating();
    assertTrue(set.contains(id1));
    assertTrue(set.contains(id2));
  }

  public void testBlockingContains() throws Throwable {
    final ObjectID id3 = new ObjectID(3);
    set.add(id1);
    set.add(id2);
    assertEquals(2, set.size());

    assertTrue(set.contains(id1));
    assertTrue(set.contains(id2));
    assertFalse(set.contains(id3));

    // test blocking..
    checkBlocking(new ContainsCaller(id3));
  }

  private void checkBlocking(MethodCaller caller) throws Throwable {
    set.startPopulating();
    for (int i = 0; i < threadCnt; i++) {
      ClientThread ct = new ClientThread(caller, set, preMon, postMon);
      ct.setDaemon(true);
      ct.start();
    }
    preMon.await();
    // here, we can be sure that all client threads a ready to call the method..
    // sleep for a while, then make sure that no thread came out of the method..
    System.err.println("\n### Sleeping...");
    Thread.sleep(10 * 1000);
    System.err.println("\n### Woke up...");
    assertEquals(threadCnt, postMon.getCount());

    // now release all client threads
    set.stopPopulating(new ObjectIDSet());
    // if all threads don't come out of the method we'll hang forever on the next line..
    postMon.await();

  }

}

abstract class MethodCaller {
  protected final ObjectID id;

  public MethodCaller(ObjectID id) {
    this.id = id;
  }

  abstract void invoke(SyncObjectIdSetImpl set);
}

class RemoveCaller extends MethodCaller {
  public RemoveCaller(ObjectID id) {
    super(id);
  }

  @Override
  public void invoke(SyncObjectIdSetImpl set) {
    set.remove(id);
  }
}

class ContainsCaller extends MethodCaller {
  public ContainsCaller(ObjectID id) {
    super(id);
  }

  @Override
  public void invoke(SyncObjectIdSetImpl set) {
    set.contains(id);
  }
}

class ClientThread extends Thread {
  private final MethodCaller        mc;
  private final SyncObjectIdSetImpl set;
  private final CountDownLatch      pre;
  private final CountDownLatch      post;

  public ClientThread(MethodCaller mc, SyncObjectIdSetImpl set, CountDownLatch pre, CountDownLatch post) {
    this.mc = mc;
    this.set = set;
    this.pre = pre;
    this.post = post;
  }

  @Override
  public void run() {
    try {
      pre.countDown();
      mc.invoke(set);
      post.countDown();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
