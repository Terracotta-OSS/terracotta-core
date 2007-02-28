/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;

public class ClientObjectManagerTest extends BaseDSOTestCase {
  private ClientObjectManager     mgr;
  private TestRemoteObjectManager remoteObjectManager;
  private DSOClientConfigHelper   clientConfiguration;
  private ObjectIDProvider        idProvider;
  private EvictionPolicy          cache;
  private RuntimeLogger           runtimeLogger;
  private ClassProvider           classProvider;
  private TCClassFactory          classFactory;
  private TestObjectFactory       objectFactory;
  private String                  rootName;
  private Object                  object;
  private ObjectID                objectID;
  private TCObject                tcObject;

  public void setUp() throws Exception {
    remoteObjectManager = new TestRemoteObjectManager();
    classProvider = new MockClassProvider();
    clientConfiguration = configHelper();
    classFactory = new TestClassFactory();
    objectFactory = new TestObjectFactory();
    cache = new NullCache();
    runtimeLogger = new NullRuntimeLogger();

    rootName = "myRoot";
    object = new Object();
    objectID = new ObjectID(1);
    tcObject = new MockTCObject(objectID, object);
    objectFactory.peerObject = object;
    objectFactory.tcObject = tcObject;

    mgr = new ClientObjectManagerImpl(remoteObjectManager, clientConfiguration, idProvider, cache, runtimeLogger,
                                      new TestChannelIDProvider(), classProvider, classFactory, objectFactory,
                                      new PortabilityImpl(clientConfiguration), null);
    mgr.setTransactionManager(new MockTransactionManager());
  }

  /**
   * Test to make sure that a root request that is blocked waiting for a server response doesn't block reconnect.
   */
  public void testLookupOrCreateRootDoesntBlockReconnect() throws Exception {
    // prepare a successful object lookup
    TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    // prepare the lookup thread
    CyclicBarrier barrier = new CyclicBarrier(1);
    LookupRootAgent lookup1 = new LookupRootAgent(barrier, mgr, rootName, object);

    // start the lookup
    new Thread(lookup1).start();
    // make sure the first caller has called down into the remote object manager
    remoteObjectManager.retrieveRootIDCalls.take();
    assertNull(remoteObjectManager.retrieveRootIDCalls.poll(0));

    // now make sure that concurrent reconnect activity doesn't block
    mgr.pause();
    mgr.starting();
    mgr.getAllObjectIDsAndClear(new HashSet());
  }

  /**
   * Test to make sure that concurrent root requests only result in a single lookup to the server.
   */
  public void testLookupOrCreateRootConcurrently() throws Exception {

    // prepare a successful object lookup
    TestDNA dna = newEmptyDNA();
    prepareObjectLookupResults(dna);

    // prepare the lookup threads.
    CyclicBarrier barrier = new CyclicBarrier(3);
    LookupRootAgent lookup1 = new LookupRootAgent(barrier, mgr, rootName, object);
    LookupRootAgent lookup2 = new LookupRootAgent(barrier, mgr, rootName, object);

    // start the first lookup
    new Thread(lookup1).start();
    // make sure the first caller has called down into the remote object manager.
    remoteObjectManager.retrieveRootIDCalls.take();
    assertNull(remoteObjectManager.retrieveRootIDCalls.poll(0));

    // now start another lookup and make sure that it doesn't call down into the remote object manager.
    new Thread(lookup2).start();
    ThreadUtil.reallySleep(5000);
    assertNull(remoteObjectManager.retrieveRootIDCalls.poll(0));

    // allow the first lookup to proceed.
    remoteObjectManager.retrieveRootIDResults.put(objectID);

    // make sure both lookups are complete.
    barrier.barrier();

    assertTrue(lookup1.success() && lookup2.success());

    // They should both have the same result
    assertEquals(lookup1, lookup2);

    // but, the remote object manager retrieveRootID() should only have been called the first time.
    assertNull(remoteObjectManager.retrieveRootIDCalls.poll(0));
  }

  private TestDNA newEmptyDNA() {
    TestDNA dna = new TestDNA();
    dna.type = Object.class;
    dna.objectID = objectID;
    dna.arraySize = 0;
    return dna;
  }

  private void prepareObjectLookupResults(TestDNA dna) {
    remoteObjectManager.retrieveResults.put(dna);
  }

  private static final class LookupRootAgent implements Runnable {
    private final ClientObjectManager manager;
    private final String              rootName;
    private final Object              object;
    public Object                     result;
    public Throwable                  exception;
    private final CyclicBarrier       barrier;

    private LookupRootAgent(CyclicBarrier barrier, ClientObjectManager manager, String rootName, Object object) {
      this.barrier = barrier;
      this.manager = manager;
      this.rootName = rootName;
      this.object = object;
    }

    public boolean success() {
      return exception == null;
    }

    public void run() {
      try {
        result = manager.lookupOrCreateRoot(this.rootName, this.object);
      } catch (Throwable t) {
        t.printStackTrace();
        this.exception = t;
      } finally {
        try {
          barrier.barrier();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public boolean equals(Object o) {
      if (o instanceof LookupRootAgent) {
        LookupRootAgent cmp = (LookupRootAgent) o;
        if (result == null) return cmp.result == null;
        return result.equals(cmp.result);
      } else return false;
    }

    public int hashCode() {
      throw new RuntimeException("Don't ask me that.");
    }

  }

  private static final class TestDNA implements DNA {

    public Class    type;
    public ObjectID objectID;
    public ObjectID parentObjectID = ObjectID.NULL_ID;
    public int      arraySize;

    public long getVersion() {
      throw new ImplementMe();
    }

    public boolean hasLength() {
      throw new ImplementMe();
    }

    public int getArraySize() {
      return this.arraySize;
    }

    public String getTypeName() {
      return getClass().getName();
    }

    public ObjectID getObjectID() throws DNAException {
      return objectID;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return parentObjectID;
    }

    public DNACursor getCursor() {
      throw new ImplementMe();
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public boolean isDelta() {
      return false;
    }

  }

}
