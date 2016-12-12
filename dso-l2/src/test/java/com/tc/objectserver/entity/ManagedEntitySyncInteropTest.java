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
import static org.junit.Assert.*;

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
    Future f1 = run(()->instance.startSync());
    Future f2 = run(()->instance.startSync());
    try {
      f1.get(1, TimeUnit.SECONDS);
      f2.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  @Test
  public void testLifecyleBlocksSync() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
// start two syncs
    Future f1 = run(()->instance.startLifecycle());
    Future f2 = run(()->instance.startSync());
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.finishLifecycle();
    try {
      f2.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  @Test
  public void testSyncStartBlockReference() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
// start two syncs
    Future f1 = run(()->instance.startSync());
    Future f2 = run(()->instance.startReference());
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.syncStarted();
    try {
      f2.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }

  @Test
  public void testSyncBlockLifecycle() throws Exception {
    ManagedEntitySyncInterop instance = new ManagedEntitySyncInterop();
// start two syncs
    Future f1 = run(()->instance.startSync());
    Future f2 = run(()->instance.startLifecycle());
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.syncStarted();
    try {
      f2.get(1, TimeUnit.SECONDS);
      Assert.fail();
    } catch (TimeoutException to) {
    // EXPECTED
    }
    instance.syncFinished();
    try {
      f2.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException to) {
      Assert.fail("should not block");
    }
  }
  
  private Future<?> run(Runnable r) {
    return service.submit(r);
  }
  
}
