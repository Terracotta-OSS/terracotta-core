/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.TestClassFactory.MockTCClass;
import com.tc.object.TestClassFactory.MockTCField;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.field.TCField;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.Counter;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;
import java.util.Map;

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
  private CyclicBarrier           mutualRefBarrier;

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
                                      new PortabilityImpl(clientConfiguration), null, null);
    mgr.setTransactionManager(new MockTransactionManager());
  }

  public void testMutualReferenceLookup() {
    mutualRefBarrier = new CyclicBarrier(2);

    remoteObjectManager = new TestRemoteObjectManager() {

      public DNA retrieve(ObjectID id) {
        try {

          mutualRefBarrier.barrier();
        } catch (BrokenBarrierException e) {
          throw new AssertionError(e);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        return new TestDNA(id);
      }
    };

    // re-init manager
    TestMutualReferenceObjectFactory testMutualReferenceObjectFactory = new TestMutualReferenceObjectFactory();
    ClientObjectManagerImpl clientObjectManager = new ClientObjectManagerImpl(remoteObjectManager, clientConfiguration, idProvider,
                                                               cache, runtimeLogger, new TestChannelIDProvider(),
                                                               classProvider, classFactory,
                                                               testMutualReferenceObjectFactory,
                                                               new PortabilityImpl(clientConfiguration), null, null);
    mgr = clientObjectManager;
    MockTransactionManager mockTransactionManager = new MockTransactionManager();
    mgr.setTransactionManager(mockTransactionManager);
    testMutualReferenceObjectFactory.setObjectManager(mgr);

    // run the threads now..
    ObjectID objectID1 = new ObjectID(1);
    ObjectID objectID2 = new ObjectID(2);
    ExceptionHolder exceptionHolder1 = new ExceptionHolder();
    ExceptionHolder exceptionHolder2 = new ExceptionHolder();
    
    LookupThread lookupThread1 = new LookupThread(objectID1, mgr, mockTransactionManager, exceptionHolder1);
    LookupThread lookupThread2 = new LookupThread(objectID2, mgr, mockTransactionManager, exceptionHolder2);
    //
    Thread t1 = new Thread(lookupThread1);
    t1.start();
    Thread t2 = new Thread(lookupThread2);
    t2.start();
    try {
      t1.join();
      t2.join();

    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    
    if(exceptionHolder1.getExceptionOccurred().get()) {
      throw new AssertionError(exceptionHolder1.getThreadException());
    }
    
    if(exceptionHolder2.getExceptionOccurred().get()) {
      throw new AssertionError(exceptionHolder2.getThreadException());
    }
    
    assertEquals(mockTransactionManager.getLoggingCounter().get(), 0);
    assertEquals(clientObjectManager.getObjectLatchStateMap().size(), 0);

  }

  private static final class LookupThread implements Runnable {

    private final ObjectID                 oid;
    private final ClientObjectManager      clientObjectManager;
    private final ClientTransactionManager clientTransactionManager;
    private final ExceptionHolder          exceptionHolder;

    public LookupThread(ObjectID oid, ClientObjectManager clientObjectManager,
                        ClientTransactionManager clientTransactionManager, ExceptionHolder exceptionHolder) {
      this.oid = oid;
      this.clientObjectManager = clientObjectManager;
      this.clientTransactionManager = clientTransactionManager;
      this.exceptionHolder = exceptionHolder;
    }

    public void run() {
      try {
        assertNotNull(clientObjectManager);
        assertNotNull(clientTransactionManager);
        assertNotNull(oid);
        assertFalse(clientTransactionManager.isTransactionLoggingDisabled());
        TestObject testObject = (TestObject) clientObjectManager.lookupObject(oid);
        assertNotNull(testObject);
        assertNotNull(testObject.object);
        assertFalse(clientTransactionManager.isTransactionLoggingDisabled());
      } catch (Exception e) {
        exceptionHolder.getExceptionOccurred().set(true);
        exceptionHolder.setThreadException(e);
      }
    }
  }
  
  private static final class ExceptionHolder {
    private final SynchronizedBoolean exceptionOccurred = new SynchronizedBoolean(false);
    private Exception threadException;
  
    public Exception getThreadException() {
      return threadException;
    }
  
    public void setThreadException(Exception threadException) {
      this.threadException = threadException;
    }
    
    public SynchronizedBoolean getExceptionOccurred() {
      return exceptionOccurred;
    }   
      
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

  private static class TestDNA implements DNA {

    public Class    type;
    public ObjectID objectID;
    public ObjectID parentObjectID = ObjectID.NULL_ID;
    public int      arraySize;

    public TestDNA() {
      // do nothing
    }

    public TestDNA(ObjectID objectID) {
      this.objectID = objectID;
    }

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

  private static class TestMutualReferenceObjectFactory implements TCObjectFactory {

    private ClientObjectManager clientObjectManager;
    private final ThreadLocal   localDepthCounter = new ThreadLocal() {

                                                    protected synchronized Object initialValue() {
                                                      return new Counter();
                                                    }

                                                  };

    public void setObjectManager(ClientObjectManager objectManager) {
      this.clientObjectManager = objectManager;
    }

    public Object getNewPeerObject(TCClass type, Object parent) throws IllegalArgumentException, SecurityException {
      return new TestObject();
    }

    public Object getNewArrayInstance(TCClass type, int size) {
      throw new ImplementMe();
    }

    public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, SecurityException {
      return new TestObject();
    }

    public Object getNewPeerObject(TCClass type, DNA dna) {
      return new TestObject();
    }

    private Counter getLocalDepthCounter() {
      return (Counter) localDepthCounter.get();
    }

    public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew) {
      throw new ImplementMe();
    }

    public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew) {
      TCObjectPhysical tcObj = null;
      if (id.toLong() == 1) {
        if (getLocalDepthCounter().get() == 0) {
          TCField[] tcFields = new TCField[] { new MockTCField("object") };
          TCClass tcClass = new MockTCClass(clientObjectManager, true, true, true, tcFields);
          tcObj = new TCObjectPhysical(clientObjectManager.getReferenceQueue(), id, null, tcClass, isNew);
          tcObj.setReference("object", new ObjectID(2));
          getLocalDepthCounter().increment();
        } else {
          TCField[] tcFields = new TCField[] { new MockTCField("object") };
          TCClass tcClass = new MockTCClass(clientObjectManager, true, true, true, tcFields);
          tcObj = new TCObjectPhysical(clientObjectManager.getReferenceQueue(), id, null, tcClass, isNew);
          getLocalDepthCounter().increment();
        }
      } else if (id.toLong() == 2) {
        if (getLocalDepthCounter().get() == 0) {
          TCField[] tcFields = new TCField[] { new MockTCField("object") };
          TCClass tcClass = new MockTCClass(clientObjectManager, true, true, true, tcFields);
          tcObj = new TCObjectPhysical(clientObjectManager.getReferenceQueue(), id, null, tcClass, isNew);
          tcObj.setReference("object", new ObjectID(1));
          getLocalDepthCounter().increment();
        } else {
          TCField[] tcFields = new TCField[] { new MockTCField("object") };
          TCClass tcClass = new MockTCClass(clientObjectManager, true, true, true, tcFields);
          tcObj = new TCObjectPhysical(clientObjectManager.getReferenceQueue(), id, null, tcClass, isNew);
          getLocalDepthCounter().increment();
        }
      }

      return tcObj;
    }

  }

  private static class TestObject implements TransparentAccess, Manageable {
    public TestObject object;
    private TCObject  tcObject;

    public void __tc_getallfields(Map map) {
      throw new ImplementMe();

    }

    public Object __tc_getmanagedfield(String name) {
      throw new ImplementMe();
    }

    public void __tc_setfield(String name, Object value) {
      if ("object".equals(name)) {
        object = (TestObject) value;
      }
    }

    public void __tc_setmanagedfield(String name, Object value) {
      throw new ImplementMe();

    }

    public boolean __tc_isManaged() {
      return this.tcObject == null ? false : true;
    }

    public void __tc_managed(TCObject t) {
      this.tcObject = t;
    }

    public TCObject __tc_managed() {
      return this.tcObject;
    }
  }

}
