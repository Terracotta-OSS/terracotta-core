/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.abortable.NullAbortableOperationManager;
import com.tc.async.impl.MockSink;
import com.tc.exception.ImplementMe;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.TestLocksRecallService;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalCacheManagerImpl;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientObjectManagerTest extends BaseDSOTestCase {
  private ClientObjectManager     mgr;
  private TestRemoteObjectManager remoteObjectManager;
  private DSOClientConfigHelper   clientConfiguration;
  private ObjectIDProvider        idProvider;
  private ClassProvider           classProvider;
  private TCClassFactory          classFactory;
  private TestObjectFactory       objectFactory;
  private String                  rootName;
  private Object                  object;
  private ObjectID                objectID;
  private MockTCObject            tcObject;
  private TCObjectSelfStore       tcObjectSelfStore;

  @Override
  public void setUp() throws Exception {
    this.remoteObjectManager = new TestRemoteObjectManager();
    this.classProvider = new MockClassProvider();
    this.classFactory = new TestClassFactory();
    this.objectFactory = new TestObjectFactory();
    this.clientConfiguration = configHelper();
    this.tcObjectSelfStore = new L1ServerMapLocalCacheManagerImpl(new TestLocksRecallService(), new MockSink(),
                                                                  new MockSink(), new MockSink());

    this.rootName = "myRoot";
    this.object = new Object();
    this.objectID = new ObjectID(1);
    this.tcObject = new MockTCObject(this.objectID, this.object);
    this.objectFactory.peerObject = this.object;
    this.objectFactory.tcObject = this.tcObject;

    this.mgr = new ClientObjectManagerImpl(this.remoteObjectManager, this.idProvider,
                                           new ClientIDProviderImpl(new TestChannelIDProvider()), this.classProvider,
                                           this.classFactory, this.objectFactory,
                                           new PortabilityImpl(this.clientConfiguration), this.tcObjectSelfStore,
                                           new NullAbortableOperationManager());
    this.mgr.setTransactionManager(new MockTransactionManager());
  }

  public void testShutdownWhilePaused() throws Exception {

    // We don't use the node or disconnected count for ClientObjectManager
    System.out.println("Pausing the ClientObjectManager");
    ((ClientHandshakeCallback) mgr).pause(null, 0);

    final AtomicBoolean pass = new AtomicBoolean(false);
    Thread waiter = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          System.out.println("Running a root replace to waitUntilRunning");
          mgr.replaceRootIDIfNecessary("foo", objectID);
          Assert.fail("Root replacement finished successfully");
        } catch (TCNotRunningException e) {
          System.out.println("Got the expected TCNotRunningException");
          pass.set(true);
        }
      }
    });
    System.out.println("Starting the root replacer.");
    waiter.setDaemon(true);
    waiter.start();

    System.out.println("Waiting until root replacer is blocked in the WAITING state");
    while (waiter.getState() != Thread.State.WAITING) {
      ThreadUtil.reallySleep(1000);
    }

    System.out.println("Shutting down the ClientObjectManager");
    mgr.shutdown(false);

    System.out.println("Waiting to join the root replacer thread.");
    waiter.join(10 * 1000);

    Assert.assertTrue("Root replacement did not throw a TCNotRUnningException!", pass.get());
  }

  public void testObjectNotFoundConcurrentLookup() throws Exception {
    final ObjectID id = new ObjectID(1);
    final List errors = Collections.synchronizedList(new ArrayList());

    final Runnable lookup = new Runnable() {
      @Override
      public void run() {
        try {
          ClientObjectManagerTest.this.mgr.lookup(id);
        } catch (final Throwable t) {
          System.err.println("got exception: " + t.getClass().getName());
          errors.add(t);
        }
      }
    };

    final Thread t1 = new Thread(lookup);
    t1.start();
    final Thread t2 = new Thread(lookup);
    t2.start();

    ThreadUtil.reallySleep(5000);

    this.remoteObjectManager.retrieveResults.put(TestRemoteObjectManager.THROW_NOT_FOUND);
    this.remoteObjectManager.retrieveResults.put(TestRemoteObjectManager.THROW_NOT_FOUND);

    t1.join();
    t2.join();

    assertEquals(2, errors.size());
    assertEquals(TCObjectNotFoundException.class, errors.remove(0).getClass());
    assertEquals(TCObjectNotFoundException.class, errors.remove(0).getClass());
  }

  public void testClassNotFoundExceptionDuringLookup() throws Exception {
    final ClassNotFoundException expect = new ClassNotFoundException();
    this.tcObject.setHydrateException(expect);

    final TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    try {
      this.mgr.lookup(this.objectID);
      fail("no exception");
    } catch (final Exception e) {
      if (!(e == expect || e.getCause() == expect)) {
        fail(e);
      } else {
        System.out.println("XXX Got exception : " + e);
      }
    }

    Assert.eval(this.remoteObjectManager.removedObjects.contains(this.objectID));
  }

  public void testExceptionDuringHydrateClearsState() throws Exception {
    final RuntimeException expect = new RuntimeException();
    this.tcObject.setHydrateException(expect);

    TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    try {
      this.mgr.lookup(this.objectID);
      fail("no exception");
    } catch (final Exception e) {
      if (!(e == expect || e.getCause() == expect)) {
        fail(e);
      }
    }

    dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    try {
      this.mgr.lookup(this.objectID);
      fail("no exception");
    } catch (final Exception e) {
      if (!(e == expect || e.getCause() == expect)) {
        fail(e);
      }
    }
  }

  /**
   * Test to make sure that a root request that is blocked waiting for a server response doesn't block reconnect.
   */
  public void testLookupOrCreateRootDoesntBlockReconnect() throws Exception {
    // prepare a successful object lookup
    final TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    // prepare the lookup thread
    final CyclicBarrier barrier = new CyclicBarrier(1);
    final LookupRootAgent lookup1 = new LookupRootAgent(barrier, this.mgr, this.rootName, this.object);

    // start the lookup
    new Thread(lookup1).start();
    // make sure the first caller has called down into the remote object manager
    this.remoteObjectManager.retrieveRootIDCalls.take();
    assertNull(this.remoteObjectManager.retrieveRootIDCalls.poll(0, TimeUnit.MILLISECONDS));
  }

  /**
   * Test to make sure that concurrent root requests only result in a single lookup to the server.
   */
  public void testLookupOrCreateRootConcurrently() throws Exception {

    // prepare a successful object lookup
    final TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    // prepare the lookup threads.
    final CyclicBarrier barrier = new CyclicBarrier(3);
    final LookupRootAgent lookup1 = new LookupRootAgent(barrier, this.mgr, this.rootName, this.object);
    final LookupRootAgent lookup2 = new LookupRootAgent(barrier, this.mgr, this.rootName, this.object);

    // start the first lookup
    new Thread(lookup1, "lookup1").start();
    // make sure the first caller has called down into the remote object manager.
    this.remoteObjectManager.retrieveRootIDCalls.take();
    assertNull(this.remoteObjectManager.retrieveRootIDCalls.poll(0, TimeUnit.MILLISECONDS));

    // now start another lookup and make sure that it doesn't call down into the remote object manager.
    new Thread(lookup2, "lookup2").start();
    ThreadUtil.reallySleep(5000);
    assertNull(this.remoteObjectManager.retrieveRootIDCalls.poll(0, TimeUnit.MILLISECONDS));

    // allow the first lookup to proceed.
    this.remoteObjectManager.retrieveRootIDResults.put(this.objectID);

    // make sure both lookups are complete.
    barrier.await();

    assertTrue(lookup1.success() && lookup2.success());

    // They should both have the same result
    assertEquals(lookup1, lookup2);

    // but, the remote object manager retrieveRootID() should only have been called the first time.
    assertNull(this.remoteObjectManager.retrieveRootIDCalls.poll(0, TimeUnit.MILLISECONDS));
  }

  private TestDNA newEmptyDNA() {
    final TestDNA dna = new TestDNA();
    dna.objectID = this.objectID;
    dna.arraySize = 0;
    return dna;
  }

  private void prepareObjectLookupResults(final TestDNA dna) {
    this.remoteObjectManager.retrieveResults.add(dna);
  }

  private static final class LookupRootAgent implements Runnable {
    private final ClientObjectManager manager;
    private final String              rootName;
    private final Object              object;
    public Object                     result;
    public Throwable                  exception;
    private final CyclicBarrier       barrier;

    private LookupRootAgent(final CyclicBarrier barrier, final ClientObjectManager manager, final String rootName,
                            final Object object) {
      this.barrier = barrier;
      this.manager = manager;
      this.rootName = rootName;
      this.object = object;
    }

    public boolean success() {
      return this.exception == null;
    }

    @Override
    public void run() {
      try {
        this.result = this.manager.lookupOrCreateRoot(this.rootName, this.object);
      } catch (final Throwable t) {
        t.printStackTrace();
        this.exception = t;
      } finally {
        try {
          this.barrier.await();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        } catch (final BrokenBarrierException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof LookupRootAgent) {
        final LookupRootAgent cmp = (LookupRootAgent) o;
        if (this.result == null) { return cmp.result == null; }
        return this.result.equals(cmp.result);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      throw new RuntimeException("Don't ask me that.");
    }

  }

  private static class TestDNA implements DNA {

    public ObjectID objectID;
    public ObjectID parentObjectID = ObjectID.NULL_ID;
    public int      arraySize;

    public TestDNA() {
      // do nothing
    }

    @Override
    public long getVersion() {
      throw new ImplementMe();
    }

    @Override
    public boolean hasLength() {
      throw new ImplementMe();
    }

    @Override
    public int getArraySize() {
      return this.arraySize;
    }

    @Override
    public String getTypeName() {
      return getClass().getName();
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return this.objectID;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return this.parentObjectID;
    }

    @Override
    public DNACursor getCursor() {
      throw new ImplementMe();
    }

    @Override
    public boolean isDelta() {
      return false;
    }
  }

  private static class StupidTestObject {
    private static final Random rndm = new Random();

    @SuppressWarnings("unused")
    private StupidTestObject    object;

    @Override
    public boolean equals(final Object o) {
      return rndm.nextBoolean();
    }

    @Override
    public int hashCode() {
      return rndm.nextInt();
    }
  }

  public void testSharedObjectWithAbsurdHashCodeEqualsBehavior() throws Exception {
    this.object = new StupidTestObject();
    this.objectID = new ObjectID(1);
    this.tcObject = new MockTCObject(this.objectID, this.object);
    this.objectFactory.peerObject = this.object;
    this.objectFactory.tcObject = this.tcObject;

    final TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    final TCObject tco = this.mgr.lookup(this.objectID);

    for (int i = 0; i < 100; i++) {
      Assert.assertSame(tco.getObjectID(), this.mgr.lookupExistingObjectID(this.object));
    }
  }

  public void testCleanupDuringLookup() throws Exception {
    final ObjectID id = new ObjectID(1);
    final List errors = Collections.synchronizedList(new ArrayList());

    final Runnable lookup = new Runnable() {
      @Override
      public void run() {
        try {
          ClientObjectManagerTest.this.mgr.lookup(id);
        } catch (final Throwable t) {
          System.err.println("got exception: " + t.getClass().getName());
          errors.add(t);
        }
      }
    };

    final Thread t1 = new Thread(lookup);
    t1.start();
    final Thread t2 = new Thread(lookup);
    t2.start();

    ThreadUtil.reallySleep(2000);
    ((ClientHandshakeCallback) this.mgr).pause(null, 0);
    ((ClientHandshakeCallback) this.mgr).cleanup();
    this.remoteObjectManager.cleanup();

    t1.join();
    t2.join();

    assertEquals(2, errors.size());
    assertEquals(PlatformRejoinException.class, errors.remove(0).getClass());
    assertEquals(PlatformRejoinException.class, errors.remove(0).getClass());

  }
}
