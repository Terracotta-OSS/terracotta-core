/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.entity;

import com.tc.util.Assert;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ManagedEntitySyncInteropTest {
  
  static ExecutorService service;
  
  public ManagedEntitySyncInteropTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
     service = Executors.newCachedThreadPool();
  }
  
  @AfterClass
  public static void tearDownClass() {
    service.shutdownNow();
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testMultipleAccessToSync() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
// start two syncs
    Future f1 = run(()->{instance.startSync();return null;});
    Future f2 = run(()->{instance.startSync();return null;});
    try {
      f1.get(5, TimeUnit.SECONDS);
      f2.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  @Test
  public void testLifecyleBlocksSync() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
    // WARNING:  run(Callable) will potentially run the given Callable instances concurrently.
    // This means that we need to wait on the startLifecycle() finishing before the startSync() since, otherwise, the
    // lifecycle will block and this test is expecting the blocking in the opposite order so it will hang.
    
    // Start the lifecycle operation
    Future f1 = run(()->{instance.startLifecycle();return null;});
    f1.get();
    // Now, start the sync (it should block).
    Future f2 = run(()->{instance.startSync();return null;});
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.finishLifecycle();
    try {
      f2.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  @Test
  public void testSyncStartBlockReference() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
    // WARNING:  run(Callable) will potentially run the given Callable instances concurrently.
    // This means that we need to wait on the startSync() finishing before the startReference() since, otherwise, the
    // sync will block and this test is expecting the blocking in the opposite order so it will hang.
    
    // Start the sync
    Future f1 = run(()->{instance.startSync();return null;});
    // Wait for the sync to start BEFORE we attempt the reference so we know it will block (and not the sync).
    f1.get();
    Future f2 = run(()->{instance.startReference();return null;});
    f1.get();
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.syncStarted();
    try {
      f2.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  /**
   * This test demonstrates that lifecycle operations are blocked once a sync is started but will progress once it is
   * finished.
   * 
   * @throws Exception An error in the test
   */
  @Test
  public void testSyncBlockLifecycle() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
    // WARNING:  run(Callable) will potentially run the given Callable instances concurrently.
    // This means that we need to wait on the startSync() finishing before the startLifecycle() since, otherwise, the
    // sync will block and this test is expecting the blocking in the opposite order so it will hang.
    
    // Start the sync
    Future f1 = run(()->{instance.startSync();return null;});
    // Wait for the sync to start BEFORE we attempt the lifecycle start so we know it will block (and not the sync).
    f1.get();
    Future f2 = run(()->{instance.startLifecycle();return null;});
    // We expect the lifecycle operation to be blocked since the sync is in-progress.
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    // We progress with the sync, meanwhile we still expect the lifecycle to be blocked.
    instance.syncStarted();
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    // We finish the sync and observe that the lifecycle operation can now complete.
    instance.syncFinished();
    try {
      f2.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }
  
  private <T> Future<T> run(Callable<T> r) {
    return service.submit(r);
  }
  
}
