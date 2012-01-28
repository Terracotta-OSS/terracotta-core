/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.async.impl.MockSink;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.ClientObjectManagerImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.RemoteObjectManager;
import com.tc.object.TCClassFactory;
import com.tc.object.TCObject;
import com.tc.object.TCObjectFactory;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.TestClassFactory;
import com.tc.object.TestObjectFactory;
import com.tc.object.TestRemoteObjectManager;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.TestLocksRecallService;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalCacheManagerImpl;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.sequence.SimpleSequence;

/**
 * Test to see if an adapted class has all of the adaptations we expect.
 */
public class DsoFinalMethodTest extends BaseDSOTestCase {
  private String                      rootName;
  private Object                      rootObject;
  private MockClientObjectManagerImpl objectManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.rootName = "testSyncRoot";
    this.rootObject = new Object();
    final ObjectID objectID = new ObjectID(1);
    final TCObject tcObject = new MockTCObject(objectID, this.rootObject);
    final TestObjectFactory objectFactory = new TestObjectFactory();
    final TCObjectSelfStore tcObjectSelfStore = new L1ServerMapLocalCacheManagerImpl(new TestLocksRecallService(),
                                                                                     new MockSink(), new MockSink());
    objectFactory.peerObject = this.rootObject;
    objectFactory.tcObject = tcObject;
    this.objectManager = new MockClientObjectManagerImpl(new MockRemoteObjectManagerImpl(), configHelper(),
                                                         new ObjectIDProviderImpl(new SimpleSequence()),
                                                         new NullRuntimeLogger(),
                                                         new ClientIDProviderImpl(new TestChannelIDProvider()),
                                                         new MockClassProvider(), new TestClassFactory(),
                                                         objectFactory, tcObjectSelfStore);

    this.objectManager.setTransactionManager(new MockTransactionManagerImpl());
  }

  @Override
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
    this.objectManager.lookupOrCreateRoot(this.rootName, this.rootObject, false);
  }

  public void testRootCreateOrReplaceWithDsoFinal() throws Exception {
    try {
      this.objectManager.lookupOrCreateRoot(this.rootName, this.rootObject, true);
      throw new AssertionError("should have thrown an AssertionError");
    } catch (final AssertionError e) {
      // expected.
    }
  }

  private static class MockClientObjectManagerImpl extends ClientObjectManagerImpl {
    public MockClientObjectManagerImpl(final RemoteObjectManager remoteObjectManager,
                                       final DSOClientConfigHelper clientConfiguration,
                                       final ObjectIDProvider idProvider, final RuntimeLogger runtimeLogger,
                                       final ClientIDProvider provider, final ClassProvider classProvider,
                                       final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                       final TCObjectSelfStore tcObjectSelfStore) {
      super(remoteObjectManager, clientConfiguration, idProvider, runtimeLogger, provider, classProvider, classFactory,
            objectFactory, new TestPortability(), null, null, tcObjectSelfStore);
    }

    @Override
    public Object lookupObject(final ObjectID objectID) {
      return null;
    }
  }

  private static class MockRemoteObjectManagerImpl extends TestRemoteObjectManager {

    @Override
    public ObjectID retrieveRootID(final String name) {
      throw new AssertionError("retrieveRootID should not be called.");
    }
  }

  private static class MockTransactionManagerImpl extends MockTransactionManager {
    @Override
    public void createRoot(final String name, final ObjectID id) {
      return;
    }

    @Override
    public void createObject(final TCObject source) {
      return;
    }

    @Override
    public ClientTransaction getCurrentTransaction() {
      return null;
    }
  }

  private static class TestPortability implements Portability {

    public NonPortableReason getNonPortableReason(final Class topLevelClass) {
      throw new ImplementMe();
    }

    public boolean isInstrumentationNotNeeded(final String name) {
      return false;
    }

    public boolean isPortableClass(final Class clazz) {
      return true;
    }

    public boolean isClassPhysicallyInstrumented(final Class clazz) {
      throw new ImplementMe();
    }

    public boolean isPortableInstance(final Object obj) {
      return true;
    }

  }

}
