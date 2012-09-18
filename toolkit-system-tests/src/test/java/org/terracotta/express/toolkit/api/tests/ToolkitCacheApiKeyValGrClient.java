/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.junit.Assert;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;

public class ToolkitCacheApiKeyValGrClient extends ToolkitStoreApiKeyValGrClient {

  private ToolkitCache cache;
  private static final int KEY_ONE                  = 1;
  private static final int VALUE_ONE                = 1;
  private static final int MAX_TIME_TO_IDLE_IN_SECS = 5;
  private static final int TWO_SECS                 = 2;
  private long             endTime;
  private long             startTime;
  private final static int SOME_RANDOM_INDEX        = END - 20;
  private final static int MAX_NO_OF_ELEMENTS       = 10;
  private static final int ONE_SEC                  = 1;
  private final int[]      keys;
  private final int[]      values;
  private final Set        evictedKeys              = new HashSet();
  private final Set        keysPutUntilNow          = new HashSet();
  private final Set        expiredKeys              = new HashSet();
  public ToolkitCacheApiKeyValGrClient(String[] args) {
    super(args);
    keys = new int[MAX_NO_OF_ELEMENTS];
    values = new int[MAX_NO_OF_ELEMENTS];
    for (int i = START; i < MAX_NO_OF_ELEMENTS; i++) {
      keys[i] = i;
      values[i] = i;
    }
  }


  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    System.err.println("Entered Test");
    // strong cache with LiteralKeyLiteralValueGenerator
    this.toolkit = toolKit;// this.toolkit it inherits from the super class
    setStrongDs(toolkit, NAME_OF_DS);
    setKeyValGeneratorAndRunTest();
    // eventual cache with LiteralKeyLiteralValueGenerator
    index = barrier.await();
    if (index == 0) {
      clearDs();
    }
    barrier.await();
    setEventualDs(toolkit, NAME_OF_DS);
    setKeyValGeneratorAndRunTest();

  }

  private void clearDs() {
    log("&&&&&&&&&&&&&&&&&&&DESTROYING CACHE&&&&&&&&&&&&&&");
    cache.destroy();
    log("Destroyed");
  }

  private void setKeyValGeneratorAndRunTest() throws Exception, InterruptedException, BrokenBarrierException {
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    runTest();
    barrier.await();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    runTest();
    barrier.await();
  }


  private void runTest() throws Exception, InterruptedException, BrokenBarrierException {
    System.err
.println("#$#$#$#$#$#$#$#$#$#$#$#$$#$#$#$#$#$Calling Super.test^&*^&^*&^*&^*&^*&^*&^*&^*&^&*^&*^*^&*&");
    super.test();
    barrier.await();
    log("#$#$#$#$#$#$#$#$#$#$#$#$$#$#$#$#$#$ calling this.test&*^&^*&^*&^*&^*&^*&^*&^*&^&*^&*^*^&*&");
    this.test();
  }

  @Override
  protected void test() throws Exception {
    testGetQuiet();
    testGetAllQuiet();
    testPutNoReturn();
    testPutIfAbsent();
    testSetPinned();
    testIsPinned();
    testUnpinAll();
    testAddListener();
    testRemoveListener();
    log("**************&&&&&&&&&&&&Test For Cache APi Passed&&&&&&&&&&&&&**************");
  }

  private void testRemoveListener() throws InterruptedException, BrokenBarrierException {
    log("Entering  testRemoveListener");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("Client 0 adding listener");
        ToolkitCacheListener speciallListener = getSpecialListener();
        cache.addListener(speciallListener);
        log("Client 0 added istnere, now it will remove it");
        cache.removeListener(speciallListener);
        log("listener removed");
        log("now doing puts");
        doSomePuts(START, END);
        log("DOne with puts");
      }
      barrier.await();
      log("Exiting testRemoveListener, all tests passed successfully ");
    } finally {
      tearDown();
    }
  }


  private ToolkitCacheListener getSpecialListener() {
    return new ToolkitCacheListener() {

      @Override
      public void onEviction(Object key) {
    Assert.fail("Listener Not removed");
      }

      @Override
      public void onExpiration(Object key) {
        Assert.fail("Listener Not removed");
              }
    };
  }

  private void testAddListener() throws InterruptedException, BrokenBarrierException {
    log("Entering testAddListener");
    setUp();
    try {
      index = barrier.await();
      cache.setConfigField(ToolkitCacheConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME, MAX_NO_OF_ELEMENTS);
      if (index == 0) {
        log("client 0 adding listener tp cache");
        addListenerToCache();
        log("client 0 doing some puts");
        doSomePuts(START, END);
        log("client 0 done putting");
        Assert.assertTrue(checkForAllKeysPut(START, END));
      }
      barrier.await();
      log("Getting out of testAddListener");
    } finally {
      tearDown();
    }
  }

  private boolean checkForAllKeysPut(int start, int end) {
    for (int keyIndex = start; keyIndex < end; keyIndex++) {
      String key = (String) keyValueGenerator.getKey(keyIndex);
      if (cache.containsKey(key) || evictedKeys.contains(key)
 || expiredKeys.contains(key)) {
        continue;
      } else {
        return false;
      }
}
    return true;
  }

  private void addListenerToCache() {
    ToolkitCacheListener listen = new ToolkitCacheListener() {
      @Override
      public void onEviction(Object key) {
        String strKey = (String) key;
        if (keysPutUntilNow.contains(strKey)) {
          evictedKeys.add(strKey);
        }
      }
      @Override
      public void onExpiration(Object key) {
        String strKey = (String)key;
        if (keysPutUntilNow.contains(strKey)) {
          expiredKeys.add(strKey);
        }
      }
    };
    cache.addListener(listen);
  }

  private void testUnpinAll() throws InterruptedException, BrokenBarrierException {
    log("entering unpinall");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("client 0 pinning keys");
        pinKeys(START, END);
        log("client 0 has pinned all keys");
        log("asserting that keys are pinned");
        Assert.assertTrue(checkKeysPinned(START, END));
        log("Unpinn keys");
        cache.unpinAll();
        log("Asserting that none of the keys is pinned");
        Assert.assertTrue(checkKeysNotPinned(START, END));
      }
      barrier.await();
      log("Exiting testUnpinall");
    } finally {
      tearDown();
    }
  }

  private void testIsPinned() throws InterruptedException, BrokenBarrierException {
    log("Entering testIsPinned");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("Client 0 checking that no key should be pinned rightnow!!");
        Assert.assertTrue(checkKeysNotPinned(START, END));
        log("no keys found pinned");
        log("Client 0 pinning keys from " + keyValueGenerator.getKey(START) + " to "
            + keyValueGenerator.getKey(SOME_RANDOM_INDEX));
      pinKeys(START, SOME_RANDOM_INDEX);
        log("Done pinning");
        log("asserting that keys from " + keyValueGenerator.getKey(START) + " to excluding "
            + keyValueGenerator.getKey(SOME_RANDOM_INDEX) + " are actually pinned");
        Assert.assertTrue(checkKeysPinned(START, SOME_RANDOM_INDEX));
        log("Asserting that keys not pinned are not actually pinned");
        Assert.assertTrue(checkKeysNotPinned(SOME_RANDOM_INDEX + 4, END));
        cache.unpinAll();
      }
      barrier.await();
    } finally {
      tearDown();
    }
  }

  private boolean checkKeysNotPinned(int startIndex, int end) {
    for (int keyIndex = startIndex; keyIndex < end; keyIndex++) {
      if (cache.isPinned(keyValueGenerator.getKey(keyIndex))) {
        System.err.println(keyValueGenerator.getKey(keyIndex) + " is pinned, but it should not be");
        return false; }
    }
    return true;
  }

  private boolean checkKeysPinned(int start, int lastIndex) {
    for (int keyIndex = start; keyIndex < lastIndex; keyIndex++) {
      if (!cache.isPinned(keyValueGenerator.getKey(keyIndex))) { return false; }
    }
    return true;
  }

  private void testSetPinned() throws InterruptedException, BrokenBarrierException {
    log("Entering testSetPinned");
    setUp();
    try {
      
      if (index == 0) {
        for (int i = START; i < END; i++) {
          cache.setPinned(keyValueGenerator.getKey(i), true);
          cache.put(keyValueGenerator.getKey(i), keyValueGenerator.getValue(i));

          Assert.assertTrue(cache.isPinned(keyValueGenerator.getKey(i)));
        }
        int noOfElementsPut = END - START;
        Assert.assertEquals(noOfElementsPut, cache.size());

        for (int i = START; i < END; i++) {
          Assert.assertNotNull(cache.get(keyValueGenerator.getKey(i)));
          Assert.assertEquals(cache.get(keyValueGenerator.getKey(i)), keyValueGenerator.getValue(i));
        }
        }
      
      cache.unpinAll();
      /*
       * cache.setConfigField(ToolkitCacheConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME, MAX_NO_OF_ELEMENTS); index =
       * barrier.await(); if (index == 0) { log("client 0 pinning keys"); pinKeys(START, END); log("keys pinned");
       * log("doing puts"); doSomePuts(START, END); log("Done with puts");
       * log("Asserting that allkeys are present locally"); Assert.assertTrue(checkAllKeysPresentLocaly(START, END));
       * cache.unpinAll(); } barrier.await(); log("Exiting testSetPinned");
       */} finally {
      tearDown();
//    cache.setConfigField(ToolkitCacheConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME,
      // ToolkitCacheConfigFields.DEFAULT_MAX_BYTES_LOCAL_HEAP);
    }
  }


  private boolean checkAllKeysPresentLocaly(int start, int end) {
    for (int keyIndex = start; keyIndex < end; keyIndex++) {
      if (((ToolkitCacheInternal) cache).unsafeLocalGet(keyValueGenerator.getKey(KEY_ONE)) == null) { return false; }
    }
    return true;
  }

  private void pinKeys(int start, int end) {
    log("Starting to pin keys");
    for (int keyIndex = start; keyIndex < end; keyIndex++) {
      log("key : " + keyValueGenerator.getKey(keyIndex) + " pinned");
      cache.setPinned(keyValueGenerator.getKey(keyIndex), true);
    }
    log("done pining keys");
  }

  private void testPutIfAbsent() throws Exception {
    log("Entering testPutIfAbsent");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("Client 0 doing putIfAbsent");
        cache.putIfAbsent(keyValueGenerator.getKey(KEY_ONE), keyValueGenerator.getValue(VALUE_ONE),
                          someRandomPastTime(), TWO_SECS, TWO_SECS);
        log("Client 0 Done with putIfAbsent");
      }
      barrier.await();
      if (index == 1) {
        log("Client Entering WaitUtil");
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            System.err.println("cache contains value: " + cache.get(keyValueGenerator.getKey(KEY_ONE)));
            return !cache.containsKey(keyValueGenerator.getKey(KEY_ONE));
          }
        });
        log("Client Exiting WaitUtil");
      }
      log("Getting out of putIfAbsent");

    } finally {
      tearDown();
    }

  }



  private boolean checkMapShouldContainKeyValuePairs(Map tempMap, int start, int someRandomIndex, int end) {
    /*
     * we had put in cache from start to someRandomIndex, so the tempMap should have a valid mapping for keys from start
     * to someRandomIndex and mapping should map to null for keys from someRandomIndex to end
     */
    for (int key = start; key < someRandomIndex; key++) {
      if (tempMap.get(keyValueGenerator.getKey(key)) == null) {
        return false;
      }
    }
    for (int key = someRandomIndex; key < end; key++) {
      if (tempMap.containsKey(keyValueGenerator.getKey(key)) && tempMap.get(keyValueGenerator.getKey(key)) != null) { return false; }
    }
    return true;
  }

  private Boolean checkCacheShouldNotContainKeyValuePairs(ToolkitCache cache, Set keys) {
    for (Object key : keys) {
      cache.get(key);
      if (cache.get(key) == null) {
        log("Not found for : " + key + " value found is :" + cache.get(key));
        continue;
      }
      log("found entry for : " + key.toString() + " value = " + cache.get(key));
      return false;
    }
    return true;
  }

  private void testGetQuiet() throws Exception {
    System.err.println("Entering GetQuiet");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("Client 0 doing puts");
        cache.put(keyValueGenerator.getKey(KEY_ONE), keyValueGenerator.getValue(KEY_ONE));
        log("client 0 done with puts");

        startTime = System.currentTimeMillis();
        endTime = startTime;
        log("Now Client 0 will try to do getQuiet for some time");
        while (timeElapsedInSecs() < MAX_TIME_TO_IDLE_IN_SECS - ONE_SEC) {
          Assert.assertNotNull(cache.getQuiet(keyValueGenerator.getKey(KEY_ONE)));
          log(": time is " + timeElapsedInSecs());
          endTime = System.currentTimeMillis();
        }
        log("Client 0 done with doing all getQuiet() calls");
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            cache.get(keyValueGenerator.getKey(KEY_ONE));
            return cache.containsKey(keyValueGenerator.getKey(KEY_ONE));
          }
        });

      }
      barrier.await();
      log("coming out of checkGetQuiet() method");
    } finally {
      tearDown();
    }
  }

  private void testGetAllQuiet() throws Exception {
    System.err.println("Entering checkGetAllQuiet");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("client 0 will perform some puts");
        doSomePuts(START, SOME_RANDOM_INDEX);
        log("Client 0 done putting values");

        startTime = System.currentTimeMillis();
        endTime = startTime;
        log("Now Client 0 will perform getALlQuiet on cache for some time");
        while (timeElapsedInSecs() < MAX_TIME_TO_IDLE_IN_SECS - ONE_SEC) {
          tempMap = cache.getAllQuiet(getKeySet(START, SOME_RANDOM_INDEX));
          Assert.assertTrue(checkMapShouldContainKeyValuePairs(tempMap, START, SOME_RANDOM_INDEX, END));
          endTime = System.currentTimeMillis();
        }
        log("CLient 0 done with doing getALLQuiet on cache");
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

          @Override
          public Boolean call() throws Exception {
            return checkCacheShouldNotContainKeyValuePairs(cache, getKeySet(SOME_RANDOM_INDEX, END));
          }

        });
      }
      barrier.await();
      log("Exiting getAllQuiet");
    } finally {
      tearDown();
    }
  }

  private void log(String string) {
    System.err.println(string);
  }

  private void testPutNoReturn() throws Exception {
    log("Entering testPutNoReturn");
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        log("Client 0 would do putNoReturn now ");
        log("time : " + someRandomPastTime());
        cache.putNoReturn(keyValueGenerator.getKey(KEY_ONE), keyValueGenerator.getValue(VALUE_ONE),
                          someRandomPastTime(),
                          TWO_SECS, TWO_SECS);
        log("in client 0, done with putNoReturn");
      }
      log("client 0 would now chek if cache still contains the key, as the TTI and TTL has expired, key should be dead.");
        /*
         * use assert here instead of waitutil
         */
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

          @Override
          public Boolean call() throws Exception {
          System.err.println("client0 can see : " + cache.get(keyValueGenerator.getKey(KEY_ONE)) + " cnotains key "
                             + cache.containsKey(keyValueGenerator.getKey(KEY_ONE)));
          return !cache.containsKey(keyValueGenerator.getKey(KEY_ONE));
          }
        });
      log("getting out of testPutNoReturn");
      barrier.await();

    } finally {
      tearDown();
    }
  
  }

  private long someRandomPastTime() {
    return (System.currentTimeMillis() / 1000) - (2 * TWO_SECS);
  }

  private long timeElapsedInSecs() {
    return (endTime - startTime) / 1000;
  }


  @Override
  public void setEventualDs(Toolkit toolkit, String name) {
    barrier = toolkit.getBarrier("myBarrier", 2);
    map = cache = toolkit.getCache(name, String.class);
    cache.setConfigField(ToolkitCacheConfigFields.MAX_TTI_SECONDS_FIELD_NAME, MAX_TIME_TO_IDLE_IN_SECS);
    store = (ToolkitStore) map;
  }

  @Override
  public void setStrongDs(Toolkit toolkit, String name) {
    barrier = toolkit.getBarrier("myBarrier", 2);
    ToolkitCacheConfigBuilder configBuilder = new ToolkitCacheConfigBuilder().consistency(Consistency.STRONG);
    map = cache = toolkit.getCache(name, configBuilder.build(), String.class);
    store = (ToolkitStore) map;
  }
  @Override
  protected void doSomePuts(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      keysPutUntilNow.add(keyValueGenerator.getKey(iterator));
      super.doSomePuts(iterator, iterator + 1);
    }
  }

}
