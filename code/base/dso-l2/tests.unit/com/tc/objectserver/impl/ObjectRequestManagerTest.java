/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.apache.commons.lang.NotImplementedException;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.BatchAndSend;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.LookupContext;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ObjectRequestCache;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.RequestedObject;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ResponseContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

public class ObjectRequestManagerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ManagedObjectStateFactory.disableSingleton(true);
    SleepycatPersistor persistor = new SleepycatPersistor(TCLogging.getLogger(ObjectRequestManagerTest.class),
                                                          new DBEnvironment(true, new File(".")),
                                                          new CustomSerializationAdapterFactory());

    ManagedObjectChangeListenerProviderImpl moclp = new ManagedObjectChangeListenerProviderImpl();
    moclp.setListener(new ManagedObjectChangeListener() {

      public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
        // NOP
      }

    });

    ManagedObjectStateFactory factory = ManagedObjectStateFactory.createInstance(moclp, persistor);
    ManagedObjectStateFactory.setInstance(factory);

    TestRequestManagedObjectResponseMessage.sendSet = new TreeSet();
    TestObjectsNotFoundMessage.sendSet = new TreeSet();

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testObjectIDSet() {
    int numOfObjects = 100;
    Set ids = createObjectSet(numOfObjects);

    ObjectIDSet oidSet = new ObjectIDSet(ids);

    Iterator<ObjectID> iter = oidSet.iterator();
    ObjectID oid1 = iter.next();
    while (iter.hasNext()) {
      ObjectID oid2 = iter.next();
      assertTrue(oid1.compareTo(oid2) == -1);
      oid1 = oid2;
    }
  }

  public void testMultipleRequestObjects() {
    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());

    int objectsToBeRequested = 47;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(i);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    // assert that there is only one request in the sink.
    assertEquals(respondSink.size(), numberOfRequestsMade);

    RespondToObjectRequestContext respondToObjectRequestContext = null;

    int numOfResponses = respondSink.size();
    assertEquals(numOfResponses, numberOfRequestsMade);

    int numOfRequestedObjects = 0;
    int numOfRespondedObjects = 0;
    for (int i = 0; i < numOfResponses; i++) {
      try {
        respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      System.out.println("respond: " + respondToObjectRequestContext);

      assertNotNull(respondToObjectRequestContext);
      numOfRespondedObjects += respondToObjectRequestContext.getObjs().size();
      numOfRequestedObjects += respondToObjectRequestContext.getRequestedObjectIDs().size();
      assertEquals(false, respondToObjectRequestContext.isServerInitiated());
      assertEquals(0, respondToObjectRequestContext.getMissingObjectIDs().size());
    }
    assertEquals(objectsToBeRequested, numOfRequestedObjects);
    assertEquals(objectsToBeRequested, numOfRespondedObjects);

  }

  public void testMultipleRequestResponseObjects() {
    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());

    int objectsToBeRequested = 100;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(i);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), numberOfRequestsMade);
    assertEquals(objectRequestManager.getTotalRequestedObjects(), objectsToBeRequested);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), numberOfRequestThreads);

    List objectResponseThreadList = new ArrayList();
    int numberOfResponseThreads = 1;
    CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                           respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    Set sendSet = TestRequestManagedObjectResponseMessage.sendSet;
    assertEquals(10, sendSet.size());

    int i = 0;
    for (Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getTotalRequestedObjects(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testMissingObjects() {

    TestObjectManager objectManager = new TestObjectManager() {

      @Override
      public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                   int maxCount) {

        Set ids = responseContext.getLookupIDs();
        Map resultsMap = new HashMap();
        ObjectIDSet missing = new ObjectIDSet(ids);

        ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(resultsMap,
                                                                                TCCollections.EMPTY_OBJECT_ID_SET,
                                                                                missing);
        responseContext.setResults(results);

        return false;
      }
    };
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());

    int objectsToBeRequested = 100;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    List objectRequestThreadList = new ArrayList();
    int numberOfRequestThreads = 10;
    CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      ClientID clientID = new ClientID(i);
      ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager, clientID,
                                                                        new ObjectRequestID(i), ids, false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), numberOfRequestsMade);
    assertEquals(objectRequestManager.getTotalRequestedObjects(), objectsToBeRequested);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), numberOfRequestThreads);

    List objectResponseThreadList = new ArrayList();
    int numberOfResponseThreads = 1;
    CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                           respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    Set sendSet = TestObjectsNotFoundMessage.sendSet;
    assertEquals(sendSet.size(), 10);

    int i = 0;
    for (Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      TestObjectsNotFoundMessage message = (TestObjectsNotFoundMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getTotalRequestedObjects(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testBatchAndSend() {

    TestMessageChannel messageChannel = new TestMessageChannel(new ChannelID(1));
    Sequence batchIDSequence = new SimpleSequence();
    BatchAndSend batchAndSend = new BatchAndSend(messageChannel, batchIDSequence.next());

    // let's test send objects
    for (int i = 0; i < 5000; i++) {
      ObjectID id = new ObjectID(i);
      ManagedObjectImpl mo = new ManagedObjectImpl(id);
      mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new BackReferences(),
               new NullObjectInstanceMonitor(), true);
      batchAndSend.sendObject(mo);

    }
  }

  public void testRequestObjects() {

    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());
    ClientID clientID = new ClientID(1);
    ObjectRequestID requestID = new ObjectRequestID(1);

    int objectsToBeRequested = 100;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    objectRequestManager.requestObjects(new ObjectRequestServerContextImpl(clientID, requestID, ids, Thread
        .currentThread().getName(), -1, false));

    RespondToObjectRequestContext respondToObjectRequestContext = null;

    int numOfRequestedObjects = 0;
    int numOfRespondedObjects = 0;
    int numOfResponses = respondSink.size();
    for (int i = 0; i < numOfResponses; i++) {
      try {
        respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }

      assertNotNull(respondToObjectRequestContext);
      numOfRespondedObjects += respondToObjectRequestContext.getObjs().size();
      numOfRequestedObjects += respondToObjectRequestContext.getRequestedObjectIDs().size();
      assertEquals(clientID, respondToObjectRequestContext.getRequestedNodeID());
      assertEquals(false, respondToObjectRequestContext.isServerInitiated());
      assertEquals(0, respondToObjectRequestContext.getMissingObjectIDs().size());
    }
    assertEquals(objectsToBeRequested, numOfRequestedObjects);
    assertEquals(objectsToBeRequested, numOfRespondedObjects);

  }

  public void testResponseObjects() {

    TestObjectManager objectManager = new TestObjectManager();
    TestDSOChannelManager channelManager = new TestDSOChannelManager();
    TestClientStateManager clientStateManager = new TestClientStateManager();
    TestSink requestSink = new TestSink();
    TestSink respondSink = new TestSink();
    ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                 clientStateManager, requestSink,
                                                                                 respondSink, new ObjectStatsRecorder());
    ClientID clientID = new ClientID(1);
    ObjectRequestID requestID = new ObjectRequestID(1);
    ObjectIDSet ids = createObjectIDSet(100);

    objectRequestManager.requestObjects(new ObjectRequestServerContextImpl(clientID, requestID, ids, Thread
        .currentThread().getName(), -1, false));

    RespondToObjectRequestContext respondToObjectRequestContext = null;
    try {
      respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(), respondToObjectRequestContext
        .getObjs(), respondToObjectRequestContext.getRequestedObjectIDs(), respondToObjectRequestContext
        .getMissingObjectIDs(), respondToObjectRequestContext.isServerInitiated(), respondToObjectRequestContext
        .getRequestDepth());

  }

  public void testContexts() {
    ClientID clientID = new ClientID(1);
    ObjectRequestID objectRequestID = new ObjectRequestID(1);
    ObjectIDSet ids = createObjectIDSet(100);
    ObjectIDSet missingIds = new ObjectIDSet();
    TestSink requestSink = new TestSink();
    Sink respondSink = new TestSink();
    Collection objs = null;

    LookupContext lookupContext = new LookupContext(clientID, objectRequestID, ids, 0, "Thread-1", false, requestSink,
                                                    respondSink);
    assertEquals(lookupContext.getLookupIDs().size(), ids.size());
    assertEquals(0, lookupContext.getMaxRequestDepth());
    assertEquals(clientID, lookupContext.getRequestedNodeID());
    assertEquals(objectRequestID, lookupContext.getRequestID());
    assertEquals("Thread-1", lookupContext.getRequestingThreadName());
    assertEquals(false, lookupContext.isServerInitiated());

    ResponseContext responseContext = new ResponseContext(clientID, objs, ids, missingIds, false, 0);
    assertEquals(clientID, responseContext.getRequestedNodeID());
  }

  public void testObjectRequestCache() {
    ObjectRequestCache c = new ObjectRequestCache(true);

    ObjectIDSet oidSet1 = createObjectIDSet(100);

    RequestedObject reqObj1 = new RequestedObject(oidSet1, 10);
    RequestedObject reqObj2 = new RequestedObject(oidSet1, 10);

    Assert.eval(reqObj1.equals(reqObj2));
    Assert.eval(reqObj1.hashCode() == reqObj2.hashCode());

    ClientID clientID1 = new ClientID(1);
    ClientID clientID2 = new ClientID(2);

    boolean testAdd = c.add(reqObj1, clientID1);
    Assert.assertTrue(testAdd);
    Assert.eval(c.cacheSize() == 1);

    testAdd = c.add(reqObj2, clientID2);
    Assert.assertFalse(testAdd);
    Assert.eval(c.cacheSize() == 1);

    ObjectIDSet oidSet2 = createObjectIDSet(50);
    RequestedObject reqObj3 = new RequestedObject(oidSet2, 20);

    testAdd = c.add(reqObj3, clientID2);
    Assert.assertTrue(testAdd);
    Assert.eval(c.cacheSize() == 2);

    Assert.assertTrue(c.contains(reqObj1));
    Assert.assertTrue(c.contains(reqObj2));
    Assert.assertTrue(c.contains(reqObj3));

    Set clients = c.clients();
    Assert.eval(clients.size() == 2);
    Assert.assertTrue(clients.contains(clientID1));
    Assert.assertTrue(clients.contains(clientID2));

    Set<ClientID> clientIds = c.getClientsForRequest(reqObj1);
    Assert.eval(clientIds.size() == 2);
    Assert.assertTrue(clientIds.contains(clientID1));
    Assert.assertTrue(clientIds.contains(clientID2));

    c.remove(reqObj1);
    Assert.eval(c.cacheSize() == 1);
    Assert.assertTrue(c.contains(reqObj3));
    Assert.assertFalse(c.contains(reqObj1));

    clients = c.clients();
    Assert.eval(clients.size() == 1);
    Assert.assertFalse(clients.contains(clientID1));
    Assert.assertTrue(clients.contains(clientID2));

    clientIds = c.getClientsForRequest(reqObj1);
    Assert.assertNull(clientIds);
  }

  private ObjectIDSet createObjectIDSet(int len) {
    Random ran = new Random();
    ObjectIDSet oidSet = new ObjectIDSet();

    for (int i = 0; i < len; i++) {
      oidSet.add(new ObjectID(ran.nextLong()));
    }
    return oidSet;
  }

  private Set createObjectSet(int numOfObjects) {
    Set set = new HashSet();
    for (int i = 1; i <= numOfObjects; i++) {
      set.add(new ObjectID(i));
    }
    return set;
  }

  private static class ObjectRequestThread extends Thread {

    private final ObjectRequestManager objectRequestManager;
    private final ClientID             clientID;
    private final ObjectRequestID      requestID;
    private final ObjectIDSet          ids;
    private final boolean              serverInitiated;
    private final CyclicBarrier        barrier;

    public ObjectRequestThread(CyclicBarrier barrier, ObjectRequestManager objectRequestManager, ClientID clientID,
                               ObjectRequestID requestID, ObjectIDSet ids, boolean serverInitiated) {
      this.objectRequestManager = objectRequestManager;
      this.clientID = clientID;
      this.requestID = requestID;
      this.ids = ids;
      this.serverInitiated = serverInitiated;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        this.barrier.barrier();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      this.objectRequestManager.requestObjects(new ObjectRequestServerContextImpl(this.clientID, this.requestID,
                                                                                  this.ids, Thread.currentThread()
                                                                                      .getName(), -1,
                                                                                  this.serverInitiated));
    }
  }

  private static class ObjectResponseThread extends Thread {

    private final ObjectRequestManager objectRequestManager;
    private final TestSink             sink;
    private final CyclicBarrier        barrier;

    public ObjectResponseThread(CyclicBarrier barrier, ObjectRequestManager objectRequestManager, TestSink sink) {
      this.objectRequestManager = objectRequestManager;
      this.sink = sink;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        this.barrier.barrier();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      RespondToObjectRequestContext respondToObjectRequestContext = null;
      int respondSinkSize = this.sink.size();
      Iterator testReqManObjResMsgIter = TestRequestManagedObjectResponseMessage.sendSet.iterator();
      for (int i = 0; i < respondSinkSize; i++) {
        try {
          respondToObjectRequestContext = (RespondToObjectRequestContext) this.sink.take();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        synchronized (this) {
          System.out.println("in the reponse thread: " + respondToObjectRequestContext);
          this.objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(),
                                                respondToObjectRequestContext.getObjs(), respondToObjectRequestContext
                                                    .getRequestedObjectIDs(), respondToObjectRequestContext
                                                    .getMissingObjectIDs(), respondToObjectRequestContext
                                                    .isServerInitiated(), respondToObjectRequestContext
                                                    .getRequestDepth());
          if (testReqManObjResMsgIter.hasNext()) {
            TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) testReqManObjResMsgIter
                .next();
            assertEquals(respondToObjectRequestContext.getObjs().size(), message.getObjects().size());
          }
        }
      }
    }
  }

  /**
   * RequestObjectManager calls: getActiveChannel(NodeID id);
   */
  private static class TestDSOChannelManager implements DSOChannelManager {

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void closeAll(Collection channelIDs) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public MessageChannel getActiveChannel(NodeID id) {
      return new TestMessageChannel(new ChannelID(((ClientID) id).toLong()));
    }

    public MessageChannel[] getActiveChannels() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public TCConnection[] getAllActiveClientConnections() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public Set getAllClientIDs() {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public String getChannelAddress(NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public ClientID getClientIDFor(ChannelID channelID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public boolean isActiveID(NodeID nodeID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActive(ClientID clientID, boolean persistent) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActive(ClientID clientID, boolean persistent, ServerID serverNodeID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

  }

  private static class TestClientStateManager implements ClientStateManager {

    private final Map clientStateMap = new HashMap();

    public void addReference(NodeID nodeID, ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public boolean hasReference(NodeID nodeID, ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void shutdownNode(NodeID deadNode) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void startupNode(NodeID nodeID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void stop() {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {

      Set<ObjectID> refs = (Set) this.clientStateMap.get(nodeID);

      if (refs == null) {
        this.clientStateMap.put(nodeID, (refs = new HashSet()));
      }

      if (refs.isEmpty()) {
        refs.addAll(oids);
        return oids;
      }

      Set<ObjectID> newReferences = new HashSet<ObjectID>();

      for (ObjectID oid : oids) {
        if (refs.add(oid)) {
          newReferences.add(oid);
        }
      }
      return newReferences;
    }

    public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, BackReferences references,
                                                         NodeID clientID, Set<ObjectID> objectIDs) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set<NodeID> getConnectedClientIDs() {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public int getReferenceCount(NodeID nodeID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferences(NodeID nodeID, Set<ObjectID> removed) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

  }

  /**
   * RequestObjectManager calls: start(); lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext
   * responseContext,int maxCount); releaseReadOnly(ManagedObject object);
   */
  private static class TestObjectManager implements ObjectManager {

    public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createRoot(String name, ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void flushAndEvict(List objects2Flush) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectIDSet getAllObjectIDs() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public int getCheckedOutCount() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public GarbageCollector getGarbageCollector() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectByIDOrNull(ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Set getRootIDs() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Map getRootNamesToIDsMap() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Iterator getRoots() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO: need to implement
    public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                 int maxCount) {

      Set ids = responseContext.getLookupIDs();
      Map resultsMap = new HashMap();
      for (Iterator iter = ids.iterator(); iter.hasNext();) {
        ObjectID id = (ObjectID) iter.next();
        ManagedObjectImpl mo = new ManagedObjectImpl(id);
        mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new BackReferences(),
                 new NullObjectInstanceMonitor(), true);
        resultsMap.put(id, mo);
      }

      ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(resultsMap,
                                                                              TCCollections.EMPTY_OBJECT_ID_SET,
                                                                              TCCollections.EMPTY_OBJECT_ID_SET);
      responseContext.setResults(results);

      return false;
    }

    public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectID lookupRootID(String name) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void notifyGCComplete(GCResultContext resultContext) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void release(PersistenceTransaction tx, ManagedObject object) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO : need to implement
    public void releaseReadOnly(ManagedObject object) {
      // do nothing, just a test
    }

    public void setStatsListener(ObjectManagerStatsListener listener) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO: need to implement
    public void start() {
      // starting...
    }

    public void stop() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void waitUntilReadyToGC() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectByID(ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createNewObjects(Set<ObjectID> ids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectIDSet getObjectIDsInCache() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAll(PersistenceTransaction tx, Collection<ManagedObject> collection) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAllReadOnly(Collection<ManagedObject> objects) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void setGarbageCollector(GarbageCollector gc) {
      throw new NotImplementedException(TestObjectManager.class);
    }

  }

  private static class TestMessageChannel implements MessageChannel {

    public List                   createMessageContexts = new ArrayList();
    public NoExceptionLinkedQueue sendQueue             = new NoExceptionLinkedQueue();
    private final ChannelID       channelID;

    public TestMessageChannel(ChannelID channelID) {
      this.channelID = channelID;
    }

    public void addAttachment(String key, Object value, boolean replace) {
      //
    }

    public void addListener(ChannelEventListener listener) {
      //
    }

    public void close() {
      //
    }

    public TCMessage createMessage(TCMessageType type) {
      if (TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE.equals(type)) {
        return new TestObjectsNotFoundMessage(this.channelID);
      } else if (TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE.equals(type)) {
        return new TestRequestManagedObjectResponseMessage(this.channelID);
      } else {
        return null;
      }
    }

    public Object getAttachment(String key) {
      return null;
    }

    public ChannelID getChannelID() {
      return this.channelID;
    }

    public TCSocketAddress getLocalAddress() {
      return null;
    }

    public TCSocketAddress getRemoteAddress() {
      return null;
    }

    public boolean isClosed() {
      return false;
    }

    public boolean isConnected() {
      return false;
    }

    public boolean isOpen() {
      return false;
    }

    public NetworkStackID open() {
      return null;
    }

    public Object removeAttachment(String key) {
      return null;
    }

    public void send(TCNetworkMessage message) {
      this.sendQueue.put(message);
    }

    public NodeID getLocalNodeID() {
      return null;
    }

    public NodeID getRemoteNodeID() {
      return null;
    }

    public void setLocalNodeID(NodeID source) {
      //      
    }

    public void setRemoteNodeID(NodeID destination) {
      //      
    }

  }

  private static class TestRequestManagedObjectResponseMessage implements RequestManagedObjectResponseMessage,
      Comparable {

    protected static Set    sendSet = new TreeSet();

    private final ChannelID channelID;

    public TestRequestManagedObjectResponseMessage(ChannelID channelID) {
      this.channelID = channelID;
    }

    public ChannelID getChannelID() {
      return this.channelID;
    }

    public long getBatchID() {
      return 0;
    }

    public Collection getObjects() {
      return null;
    }

    public ObjectStringSerializer getSerializer() {
      return null;
    }

    public int getTotal() {
      return 0;
    }

    public void initialize(TCByteBuffer[] dnas, int count, ObjectStringSerializer serializer, long bid, int tot) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
      return null;
    }

    public ClientID getClientID() {
      return null;
    }

    public SessionID getLocalSessionID() {
      return null;
    }

    public TCMessageType getMessageType() {
      return null;
    }

    public int getTotalLength() {
      return 0;
    }

    public void hydrate() {
      //
    }

    public void send() {
      sendSet.add(this);
    }

    public int compareTo(Object o) {
      Long value1 = getChannelID().toLong();
      Long value2 = ((TestRequestManagedObjectResponseMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

    public void doRecycleOnRead() {
      //      
    }

    public NodeID getDestinationNodeID() {
      return null;
    }

    public NodeID getSourceNodeID() {
      return null;
    }

  }

  private static class TestObjectsNotFoundMessage implements ObjectsNotFoundMessage, Comparable {

    protected static Set    sendSet = new TreeSet();

    private final ChannelID channelID;

    public TestObjectsNotFoundMessage(ChannelID channelID) {
      this.channelID = channelID;
    }

    public ChannelID getChannelID() {
      return this.channelID;
    }

    public long getBatchID() {
      return 0;
    }

    public Set getMissingObjectIDs() {
      return null;
    }

    public void initialize(Set missingObjectIDs, long batchId) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
      return null;
    }

    public ClientID getClientID() {
      return null;
    }

    public SessionID getLocalSessionID() {
      return null;
    }

    public TCMessageType getMessageType() {
      return null;
    }

    public int getTotalLength() {
      return 0;
    }

    public void hydrate() {
      //
    }

    public void send() {
      sendSet.add(this);
    }

    public int compareTo(Object o) {
      Long value1 = getChannelID().toLong();
      Long value2 = ((TestObjectsNotFoundMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

    public NodeID getDestinationNodeID() {
      return null;
    }

    public NodeID getSourceNodeID() {
      return null;
    }

  }

}
