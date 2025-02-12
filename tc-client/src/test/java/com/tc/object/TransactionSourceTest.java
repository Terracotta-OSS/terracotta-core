/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object;

import com.tc.object.tx.TransactionID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class TransactionSourceTest {

  public TransactionSourceTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of initiate method, of class TransactionSource.
   */
  @Test
  public void testConcurrentAdvancingLowWatermark() throws Exception {
    System.out.println("concurrent");
    TransactionSource instance = new TransactionSource();
    ExecutorService service = Executors.newCachedThreadPool();
    AtomicLong oldestTransaction = new AtomicLong(-1L);
    CompletableFuture<Boolean> seed = CompletableFuture.completedFuture(Boolean.TRUE);
    for (int x=0;x<64;x++) {
      CompletableFuture<Boolean> target = new CompletableFuture<>();
      seed = seed.thenCombine(target, Boolean::logicalAnd);
      service.submit(()->{
        try {
          for (int i=0;i<10000;i++) {
            TransactionID current = instance.create();
            TransactionID oldest = instance.oldest();
            Assert.assertTrue(current + " " + oldest, current.toLong() >= oldest.toLong());
            long oldT = oldestTransaction.get();
            Assert.assertTrue(Thread.currentThread().toString() + " " + oldT + " " + current.toLong() + " ", oldT <= current.toLong());
            while (oldT < oldest.toLong()) {
              if (!oldestTransaction.compareAndSet(oldT, oldest.toLong())) {
                oldT = oldestTransaction.get();
              }
            }
            if (!instance.retire(current)) {
              throw new AssertionError("failed " + Thread.currentThread().toString() + " " + current.toLong());
            }
          }
          target.complete(Boolean.TRUE);
          return true;
        } catch (Throwable t) {
          target.completeExceptionally(t);
          return false;
        }
      });
    }
    Assert.assertTrue(seed.get());
    service.shutdown();
    Assert.assertTrue(instance.create().toLong() == instance.oldest().toLong());
    System.out.println(instance.oldest());
    Assert.assertTrue(service.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  public void testOldestDoesNotAssert() {
    TransactionSource instance = new TransactionSource();
    Assert.assertTrue(new TransactionID(instance.oldest().toLong()).isValid());
  }
}
