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
import com.tc.object.TestDNACursor;
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
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.BatchAndSend;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.LookupContext;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ObjectRequestCache;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.RequestedObject;
import com.tc.objectserver.impl.ObjectRequestManagerImpl.ResponseContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.db.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
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
    final DBPersistorImpl persistor = new DBPersistorImpl(TCLogging.getLogger(ObjectRequestManagerTest.class),
                                                          new BerkeleyDBEnvironment(true, new File(".")),
                                                          new CustomSerializationAdapterFactory());

    final ManagedObjectChangeListenerProviderImpl moclp = new ManagedObjectChangeListenerProviderImpl();
    moclp.setListener(new ManagedObjectChangeListener() {

      public void changed(final ObjectID changedObject, final ObjectID oldReference, final ObjectID newReference) {
        // NOP
      }

    });

    final ManagedObjectStateFactory factory = ManagedObjectStateFactory.createInstance(moclp, persistor);
    ManagedObjectStateFactory.setInstance(factory);

    TestRequestManagedObjectResponseMessage.sendSet = new TreeSet();
    TestObjectsNotFoundMessage.sendSet = new TreeSet();

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testObjectIDSet() {
    final int numOfObjects = 100;
    final Set ids = createObjectSet(numOfObjects);

    final ObjectIDSet oidSet = new ObjectIDSet(ids);

    final Iterator<ObjectID> iter = oidSet.iterator();
    ObjectID oid1 = iter.next();
    while (iter.hasNext()) {
      final ObjectID oid2 = iter.next();
      assertTrue(oid1.compareTo(oid2) == -1);
      oid1 = oid2;
    }
  }

  public void testMultipleRequestObjects() {
    final TestObjectManager objectManager = new TestObjectManager();
    final TestDSOChannelManager channelManager = new TestDSOChannelManager();
    final TestClientStateManager clientStateManager = new TestClientStateManager();
    final TestSink requestSink = new TestSink();
    final TestSink respondSink = new TestSink();
    final ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                       clientStateManager, requestSink,
                                                                                       respondSink,
                                                                                       new ObjectStatsRecorder());

    final int objectsToBeRequested = 47;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    final ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    final List objectRequestThreadList = new ArrayList();
    final int numberOfRequestThreads = 10;
    final CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      final ClientID clientID = new ClientID(i);
      final ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager,
                                                                              clientID, new ObjectRequestID(i), ids,
                                                                              false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    // assert that there is only one request in the sink.
    assertEquals(respondSink.size(), numberOfRequestsMade);

    RespondToObjectRequestContext respondToObjectRequestContext = null;

    final int numOfResponses = respondSink.size();
    assertEquals(numOfResponses, numberOfRequestsMade);

    int numOfRequestedObjects = 0;
    int numOfRespondedObjects = 0;
    for (int i = 0; i < numOfResponses; i++) {
      try {
        respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
      } catch (final InterruptedException e) {
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
    final TestObjectManager objectManager = new TestObjectManager();
    final TestDSOChannelManager channelManager = new TestDSOChannelManager();
    final TestClientStateManager clientStateManager = new TestClientStateManager();
    final TestSink requestSink = new TestSink();
    final TestSink respondSink = new TestSink();
    final ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                       clientStateManager, requestSink,
                                                                                       respondSink,
                                                                                       new ObjectStatsRecorder());

    final int objectsToBeRequested = 100;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    final ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    final List objectRequestThreadList = new ArrayList();
    final int numberOfRequestThreads = 10;
    final CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      final ClientID clientID = new ClientID(i);
      final ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager,
                                                                              clientID, new ObjectRequestID(i), ids,
                                                                              false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), numberOfRequestsMade);
    assertEquals(objectRequestManager.getTotalRequestedObjects(), objectsToBeRequested);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), numberOfRequestThreads);

    final List objectResponseThreadList = new ArrayList();
    final int numberOfResponseThreads = 1;
    final CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      final ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                                 respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (final Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      final ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (final Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      final ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    final Set sendSet = TestRequestManagedObjectResponseMessage.sendSet;
    assertEquals(10, sendSet.size());

    int i = 0;
    for (final Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      final TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getTotalRequestedObjects(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testMissingObjects() {

    final TestObjectManager objectManager = new TestObjectManager() {

      @Override
      public boolean lookupObjectsAndSubObjectsFor(final NodeID nodeID,
                                                   final ObjectManagerResultsContext responseContext, final int maxCount) {

        final Set ids = responseContext.getLookupIDs();
        final Map resultsMap = new HashMap();
        final ObjectIDSet missing = new ObjectIDSet(ids);

        final ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(
                                                                                      resultsMap,
                                                                                      TCCollections.EMPTY_OBJECT_ID_SET,
                                                                                      missing);
        responseContext.setResults(results);

        return false;
      }
    };
    final TestDSOChannelManager channelManager = new TestDSOChannelManager();
    final TestClientStateManager clientStateManager = new TestClientStateManager();
    final TestSink requestSink = new TestSink();
    final TestSink respondSink = new TestSink();
    final ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                       clientStateManager, requestSink,
                                                                                       respondSink,
                                                                                       new ObjectStatsRecorder());

    final int objectsToBeRequested = 100;
    int numberOfRequestsMade = objectsToBeRequested / ObjectRequestManagerImpl.SPLIT_SIZE;
    if (objectsToBeRequested % ObjectRequestManagerImpl.SPLIT_SIZE > 0) {
      numberOfRequestsMade++;
    }
    final ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    final List objectRequestThreadList = new ArrayList();
    final int numberOfRequestThreads = 10;
    final CyclicBarrier requestBarrier = new CyclicBarrier(numberOfRequestThreads);

    for (int i = 0; i < numberOfRequestThreads; i++) {
      final ClientID clientID = new ClientID(i);
      final ObjectRequestThread objectRequestThread = new ObjectRequestThread(requestBarrier, objectRequestManager,
                                                                              clientID, new ObjectRequestID(i), ids,
                                                                              false);
      objectRequestThreadList.add(objectRequestThread);
    }

    // let's now start until all the request threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (final Iterator iter = objectRequestThreadList.iterator(); iter.hasNext();) {
      final ObjectRequestThread thread = (ObjectRequestThread) iter.next();
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    System.out.println("done doing requests.");
    assertEquals(respondSink.size(), numberOfRequestsMade);
    assertEquals(objectRequestManager.getTotalRequestedObjects(), objectsToBeRequested);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), numberOfRequestThreads);

    final List objectResponseThreadList = new ArrayList();
    final int numberOfResponseThreads = 1;
    final CyclicBarrier responseBarrier = new CyclicBarrier(numberOfResponseThreads);

    for (int i = 0; i < numberOfResponseThreads; i++) {
      final ObjectResponseThread objectResponseThread = new ObjectResponseThread(responseBarrier, objectRequestManager,
                                                                                 respondSink);
      objectResponseThreadList.add(objectResponseThread);
    }

    // let's now start until all the response threads
    for (final Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      final ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      thread.start();
    }

    // now wait for all the threads
    for (final Iterator iter = objectResponseThreadList.iterator(); iter.hasNext();) {
      final ObjectResponseThread thread = (ObjectResponseThread) iter.next();
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    final Set sendSet = TestObjectsNotFoundMessage.sendSet;
    assertEquals(sendSet.size(), 10);

    int i = 0;
    for (final Iterator iter = sendSet.iterator(); iter.hasNext(); i++) {
      final TestObjectsNotFoundMessage message = (TestObjectsNotFoundMessage) iter.next();
      System.out.println("ChannelID: " + message.getChannelID().toLong());
      assertEquals(message.getChannelID().toLong(), i);

    }

    assertEquals(objectRequestManager.getTotalRequestedObjects(), 0);
    assertEquals(objectRequestManager.getObjectRequestCacheClientSize(), 0);

  }

  public void testBatchAndSend() {

    final TestMessageChannel messageChannel = new TestMessageChannel(new ChannelID(1));
    final Sequence batchIDSequence = new SimpleSequence();
    final BatchAndSend batchAndSend = new BatchAndSend(messageChannel, batchIDSequence.next());

    // let's test send objects
    for (int i = 0; i < 5000; i++) {
      final ObjectID id = new ObjectID(i);
      final ManagedObjectImpl mo = new ManagedObjectImpl(id);
      mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new ApplyTransactionInfo(),
               new NullObjectInstanceMonitor(), true);
      batchAndSend.sendObject(mo);

    }
  }

  public void testRequestObjects() {

    final TestObjectManager objectManager = new TestObjectManager();
    final TestDSOChannelManager channelManager = new TestDSOChannelManager();
    final TestClientStateManager clientStateManager = new TestClientStateManager();
    final TestSink requestSink = new TestSink();
    final TestSink respondSink = new TestSink();
    final ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                       clientStateManager, requestSink,
                                                                                       respondSink,
                                                                                       new ObjectStatsRecorder());
    final ClientID clientID = new ClientID(1);
    final ObjectRequestID requestID = new ObjectRequestID(1);

    final int objectsToBeRequested = 100;
    final ObjectIDSet ids = createObjectIDSet(objectsToBeRequested);

    objectRequestManager.requestObjects(new ObjectRequestServerContextImpl(clientID, requestID, ids, Thread
        .currentThread().getName(), -1, false));

    RespondToObjectRequestContext respondToObjectRequestContext = null;

    int numOfRequestedObjects = 0;
    int numOfRespondedObjects = 0;
    final int numOfResponses = respondSink.size();
    for (int i = 0; i < numOfResponses; i++) {
      try {
        respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
      } catch (final InterruptedException e) {
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

    final TestObjectManager objectManager = new TestObjectManager();
    final TestDSOChannelManager channelManager = new TestDSOChannelManager();
    final TestClientStateManager clientStateManager = new TestClientStateManager();
    final TestSink requestSink = new TestSink();
    final TestSink respondSink = new TestSink();
    final ObjectRequestManagerImpl objectRequestManager = new ObjectRequestManagerImpl(objectManager, channelManager,
                                                                                       clientStateManager, requestSink,
                                                                                       respondSink,
                                                                                       new ObjectStatsRecorder());
    final ClientID clientID = new ClientID(1);
    final ObjectRequestID requestID = new ObjectRequestID(1);
    final ObjectIDSet ids = createObjectIDSet(100);

    objectRequestManager.requestObjects(new ObjectRequestServerContextImpl(clientID, requestID, ids, Thread
        .currentThread().getName(), -1, false));

    RespondToObjectRequestContext respondToObjectRequestContext = null;
    try {
      respondToObjectRequestContext = (RespondToObjectRequestContext) respondSink.take();
    } catch (final InterruptedException e) {
      throw new AssertionError(e);
    }

    objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(),
                                     respondToObjectRequestContext.getObjs(),
                                     respondToObjectRequestContext.getRequestedObjectIDs(),
                                     respondToObjectRequestContext.getMissingObjectIDs(),
                                     respondToObjectRequestContext.isServerInitiated(),
                                     respondToObjectRequestContext.getRequestDepth());

  }

  public void testContexts() {
    final ClientID clientID = new ClientID(1);
    final ObjectRequestID objectRequestID = new ObjectRequestID(1);
    final ObjectIDSet ids = createObjectIDSet(100);
    final ObjectIDSet missingIds = new ObjectIDSet();
    final TestSink requestSink = new TestSink();
    final Sink respondSink = new TestSink();
    final Collection objs = null;

    final LookupContext lookupContext = new LookupContext(clientID, objectRequestID, ids, 0, "Thread-1", false,
                                                          requestSink, respondSink);
    assertEquals(lookupContext.getLookupIDs().size(), ids.size());
    assertEquals(0, lookupContext.getMaxRequestDepth());
    assertEquals(clientID, lookupContext.getRequestedNodeID());
    assertEquals(objectRequestID, lookupContext.getRequestID());
    assertEquals("Thread-1", lookupContext.getRequestingThreadName());
    assertEquals(false, lookupContext.isServerInitiated());

    final ResponseContext responseContext = new ResponseContext(clientID, objs, ids, missingIds, false, 0);
    assertEquals(clientID, responseContext.getRequestedNodeID());
  }

  public void testObjectRequestCache() {
    final ObjectRequestCache c = new ObjectRequestCache(true);

    final ObjectIDSet oidSet1 = createObjectIDSet(100);

    final RequestedObject reqObj1 = new RequestedObject(oidSet1, 10);
    final RequestedObject reqObj2 = new RequestedObject(oidSet1, 10);

    Assert.eval(reqObj1.equals(reqObj2));
    Assert.eval(reqObj1.hashCode() == reqObj2.hashCode());

    final ClientID clientID1 = new ClientID(1);
    final ClientID clientID2 = new ClientID(2);

    boolean testAdd = c.add(reqObj1, clientID1);
    Assert.assertTrue(testAdd);
    Assert.eval(c.cacheSize() == 1);

    testAdd = c.add(reqObj2, clientID2);
    Assert.assertFalse(testAdd);
    Assert.eval(c.cacheSize() == 1);

    final ObjectIDSet oidSet2 = createObjectIDSet(50);
    final RequestedObject reqObj3 = new RequestedObject(oidSet2, 20);

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

  private ObjectIDSet createObjectIDSet(final int len) {
    final Random ran = new Random();
    final ObjectIDSet oidSet = new ObjectIDSet();

    for (int i = 0; i < len; i++) {
      oidSet.add(new ObjectID(ran.nextInt(Integer.MAX_VALUE)));
    }
    return oidSet;
  }

  private Set createObjectSet(final int numOfObjects) {
    final Set set = new HashSet();
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

    public ObjectRequestThread(final CyclicBarrier barrier, final ObjectRequestManager objectRequestManager,
                               final ClientID clientID, final ObjectRequestID requestID, final ObjectIDSet ids,
                               final boolean serverInitiated) {
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
      } catch (final Exception e) {
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

    public ObjectResponseThread(final CyclicBarrier barrier, final ObjectRequestManager objectRequestManager,
                                final TestSink sink) {
      this.objectRequestManager = objectRequestManager;
      this.sink = sink;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        this.barrier.barrier();
      } catch (final Exception e) {
        throw new AssertionError(e);
      }
      RespondToObjectRequestContext respondToObjectRequestContext = null;
      final int respondSinkSize = this.sink.size();
      final Iterator testReqManObjResMsgIter = TestRequestManagedObjectResponseMessage.sendSet.iterator();
      for (int i = 0; i < respondSinkSize; i++) {
        try {
          respondToObjectRequestContext = (RespondToObjectRequestContext) this.sink.take();
        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
        synchronized (this) {
          System.out.println("in the reponse thread: " + respondToObjectRequestContext);
          this.objectRequestManager.sendObjects(respondToObjectRequestContext.getRequestedNodeID(),
                                                respondToObjectRequestContext.getObjs(),
                                                respondToObjectRequestContext.getRequestedObjectIDs(),
                                                respondToObjectRequestContext.getMissingObjectIDs(),
                                                respondToObjectRequestContext.isServerInitiated(),
                                                respondToObjectRequestContext.getRequestDepth());
          if (testReqManObjResMsgIter.hasNext()) {
            final TestRequestManagedObjectResponseMessage message = (TestRequestManagedObjectResponseMessage) testReqManObjResMsgIter
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

    public void addEventListener(final DSOChannelManagerEventListener listener) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void closeAll(final Collection channelIDs) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public MessageChannel getActiveChannel(final NodeID id) {
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

    public String getChannelAddress(final NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public ClientID getClientIDFor(final ChannelID channelID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public boolean isActiveID(final NodeID nodeID) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActive(final ClientID clientID, final boolean persistent) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelActiveNoAck(final MessageChannel channel) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(final NodeID nid) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

    public void makeChannelRefuse(ClientID clientID, String message) {
      throw new NotImplementedException(TestDSOChannelManager.class);
    }

  }

  private static class TestClientStateManager implements ClientStateManager {

    private final Map clientStateMap = new HashMap();

    public void addReference(final NodeID nodeID, final ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public boolean hasReference(final NodeID nodeID, final ObjectID objectID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void shutdownNode(final NodeID deadNode) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public boolean startupNode(final NodeID nodeID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> rescueIds) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set<ObjectID> addReferences(final NodeID nodeID, final Set<ObjectID> oids) {

      Set<ObjectID> refs = (Set) this.clientStateMap.get(nodeID);

      if (refs == null) {
        this.clientStateMap.put(nodeID, (refs = new HashSet()));
      }

      if (refs.isEmpty()) {
        refs.addAll(oids);
        return oids;
      }

      final Set<ObjectID> newReferences = new HashSet<ObjectID>();

      for (final ObjectID oid : oids) {
        if (refs.add(oid)) {
          newReferences.add(oid);
        }
      }
      return newReferences;
    }

    public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                         final ApplyTransactionInfo references, final NodeID clientID,
                                                         final Set<ObjectID> objectIDs, final Set<ObjectID> invalidIDs) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public Set<NodeID> getConnectedClientIDs() {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public int getReferenceCount(final NodeID nodeID) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferencedFrom(final NodeID nodeID, final Set<ObjectID> secondPass) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

    public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
      throw new NotImplementedException(TestClientStateManager.class);
    }

  }

  /**
   * RequestObjectManager calls: start(); lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext
   * responseContext,int maxCount); releaseReadOnly(ManagedObject object);
   */
  private static class TestObjectManager implements ObjectManager {

    public void addFaultedObject(final ObjectID oid, final ManagedObject mo, final boolean removeOnRelease) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createRoot(final String name, final ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void flushAndEvict(final List objects2Flush) {
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
    public boolean lookupObjectsAndSubObjectsFor(final NodeID nodeID,
                                                 final ObjectManagerResultsContext responseContext, final int maxCount) {

      final Set ids = responseContext.getLookupIDs();
      final Map resultsMap = new HashMap();
      for (final Iterator iter = ids.iterator(); iter.hasNext();) {
        final ObjectID id = (ObjectID) iter.next();
        final ManagedObjectImpl mo = new ManagedObjectImpl(id);
        mo.apply(new TestDNA(new TestDNACursor()), new TransactionID(id.toLong()), new ApplyTransactionInfo(),
                 new NullObjectInstanceMonitor(), true);
        resultsMap.put(id, mo);
      }

      final ObjectManagerLookupResults results = new ObjectManagerLookupResultsImpl(resultsMap,
                                                                                    TCCollections.EMPTY_OBJECT_ID_SET,
                                                                                    TCCollections.EMPTY_OBJECT_ID_SET);
      responseContext.setResults(results);

      return false;
    }

    public boolean lookupObjectsFor(final NodeID nodeID, final ObjectManagerResultsContext context) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectID lookupRootID(final String name) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void notifyGCComplete(final GCResultContext resultContext) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAndCommit(final PersistenceTransaction tx, final ManagedObject object) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    // TODO : need to implement
    public void releaseReadOnly(final ManagedObject object) {
      // do nothing, just a test
    }

    public void setStatsListener(final ObjectManagerStatsListener listener) {
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

    public ManagedObject getObjectByID(final ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void createNewObjects(final Set<ObjectID> ids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectIDSet getObjectIDsInCache() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void preFetchObjectsAndCreate(final Set<ObjectID> oids, final Set<ObjectID> newOids) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAllAndCommit(final PersistenceTransaction tx, final Collection<ManagedObject> collection) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public void releaseAllReadOnly(final Collection<ManagedObject> objects) {
      // To Nothing
    }

    public void setGarbageCollector(final GarbageCollector gc) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ObjectIDSet getObjectReferencesFrom(final ObjectID id, final boolean cacheOnly) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public int getLiveObjectCount() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public int getCachedObjectCount() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public Iterator getRootNames() {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObjectFacade lookupFacade(final ObjectID id, final int limit) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getObjectByIDOrNull(final ObjectID id) {
      throw new NotImplementedException(TestObjectManager.class);
    }

    public ManagedObject getQuietObjectByID(ObjectID id) {
      return getObjectByID(id);
    }

  }

  private static class TestMessageChannel implements MessageChannel {

    public NoExceptionLinkedQueue sendQueue = new NoExceptionLinkedQueue();
    private final ChannelID       channelID;

    public TestMessageChannel(final ChannelID channelID) {
      this.channelID = channelID;
    }

    public void addAttachment(final String key, final Object value, final boolean replace) {
      //
    }

    public void addListener(final ChannelEventListener listener) {
      //
    }

    public void close() {
      //
    }

    public TCMessage createMessage(final TCMessageType type) {
      if (TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE.equals(type)) {
        return new TestObjectsNotFoundMessage(this.channelID);
      } else if (TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE.equals(type)) {
        return new TestRequestManagedObjectResponseMessage(this.channelID);
      } else {
        return null;
      }
    }

    public Object getAttachment(final String key) {
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

    public Object removeAttachment(final String key) {
      return null;
    }

    public void send(final TCNetworkMessage message) {
      this.sendQueue.put(message);
    }

    public NodeID getLocalNodeID() {
      return null;
    }

    public NodeID getRemoteNodeID() {
      return null;
    }

    public void setLocalNodeID(final NodeID source) {
      //
    }

  }

  private static class TestRequestManagedObjectResponseMessage implements RequestManagedObjectResponseMessage,
      Comparable {

    protected static Set    sendSet = new TreeSet();

    private final ChannelID channelID;

    public TestRequestManagedObjectResponseMessage(final ChannelID channelID) {
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

    public void initialize(final TCByteBuffer[] dnas, final int count, final ObjectStringSerializer serializer,
                           final long bid, final int tot) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
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

    public int compareTo(final Object o) {
      if (this.equals(o)) { return 0; }
      final Long value1 = getChannelID().toLong();
      final Long value2 = ((TestRequestManagedObjectResponseMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((channelID == null) ? 0 : channelID.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TestRequestManagedObjectResponseMessage other = (TestRequestManagedObjectResponseMessage) obj;
      if (channelID == null) {
        if (other.channelID != null) return false;
      } else if (channelID.toLong() != other.channelID.toLong()) return false;
      return true;
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

    public TestObjectsNotFoundMessage(final ChannelID channelID) {
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

    public void initialize(final Set missingObjectIDs, final long batchId) {
      //
    }

    public void dehydrate() {
      //
    }

    public MessageChannel getChannel() {
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

    public int compareTo(final Object o) {
      final Long value1 = getChannelID().toLong();
      final Long value2 = ((TestObjectsNotFoundMessage) o).getChannelID().toLong();
      return value1.compareTo(value2);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((channelID == null) ? 0 : channelID.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TestObjectsNotFoundMessage other = (TestObjectsNotFoundMessage) obj;
      if (channelID == null) {
        if (other.channelID != null) return false;
      } else if (!channelID.equals(other.channelID)) return false;
      return true;
    }

    public NodeID getDestinationNodeID() {
      return null;
    }

    public NodeID getSourceNodeID() {
      return null;
    }

  }

}
