/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientObjectManagerImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.RemoteObjectManager;
import com.tc.object.TCClassFactory;
import com.tc.object.TCObject;
import com.tc.object.TCObjectFactory;
import com.tc.object.TestClassFactory;
import com.tc.object.TestObjectFactory;
import com.tc.object.TestRemoteObjectManager;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.sequence.SimpleSequence;

/**
 * Test to see if an adapted class has all of the adaptations we expect.
 */
public class DsoFinalMethodTest extends BaseDSOTestCase {
  private String rootName;
  private Object rootObject;
  private MockClientObjectManagerImpl objectManager;

  protected void setUp() throws Exception {
    super.setUp();
    
    rootName = "testSyncRoot";
    rootObject = new Object();
    ObjectID objectID = new ObjectID(1);
    TCObject tcObject = new MockTCObject(objectID, rootObject);
    TestObjectFactory objectFactory = new TestObjectFactory();
    objectFactory.peerObject = rootObject;
    objectFactory.tcObject = tcObject;
    objectManager = new MockClientObjectManagerImpl(new MockRemoteObjectManagerImpl(), configHelper(), new ObjectIDProviderImpl(new SimpleSequence()), new NullCache(), new NullRuntimeLogger(),
                                                    new TestChannelIDProvider(), new MockClassProvider(), new TestClassFactory(), objectFactory);
    
    objectManager.setTransactionManager(new MockTransactionManagerImpl());
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.objectManager = null;
    this.rootName = null;
    this.rootObject = null;
  }

  /**
   * This test makes sure that method retrieveRootID() of RemoteObjectManager will not be called when dsoFinal is set to
   * false when a root is being created.
   */
  public void testRootCreateOrReplaceWithNonDsoFinal() throws Exception {
    // If retrieveRootID() of RemoteObjectManager is being called, this call will throw an
    // AssertionError.
    objectManager.lookupOrCreateRoot(rootName, rootObject, false);
  }
  
  public void testRootCreateOrReplaceWithDsoFinal() throws Exception {
    try {
      objectManager.lookupOrCreateRoot(rootName, rootObject, true);
      throw new AssertionError("should have thrown an AssertionError");
    } catch (AssertionError e) {
      // expected.
    }
  }

  private static class MockClientObjectManagerImpl extends ClientObjectManagerImpl {
    public MockClientObjectManagerImpl(RemoteObjectManager remoteObjectManager,
                                       DSOClientConfigHelper clientConfiguration, ObjectIDProvider idProvider,
                                       EvictionPolicy cache, RuntimeLogger runtimeLogger, ChannelIDProvider provider,
                                       ClassProvider classProvider, TCClassFactory classFactory,
                                       TCObjectFactory objectFactory) {
      super(remoteObjectManager, clientConfiguration, idProvider, cache, runtimeLogger, provider, classProvider,
            classFactory, objectFactory, new TestPortability());
    }

    public boolean isPortable(Object obj) {
      return true;
    }

    public Object lookupObject(ObjectID objectID) {
      return null;
    }
  }

  private static class MockRemoteObjectManagerImpl extends TestRemoteObjectManager {

    public ObjectID retrieveRootID(String name) {
      throw new AssertionError("retrieveRootID should not be called.");
    }
  }

  private static class MockTransactionManagerImpl extends MockTransactionManager {
    public void createRoot(String name, ObjectID id) {
      return;
    }
    
    public void createObject(TCObject source) {
      return;
    }
  }

  private static class TestPortability implements Portability {

    public boolean allowSubclassOf(Class clazz) {
      return true;
    }

    public NonPortableReason getNonPortableReason(Class topLevelClass) {
      throw new ImplementMe();
    }

    public boolean isInstrumentationNotNeeded(String name) {
      return false;
    }

    public boolean isPortableClass(Class clazz) {
      return true;
    }

    public boolean isClassPhysicallyInstrumented(Class clazz) {
      throw new ImplementMe();
    }

    public boolean isPortableInstance(Object obj) {
      return true;
    }

  }

}
