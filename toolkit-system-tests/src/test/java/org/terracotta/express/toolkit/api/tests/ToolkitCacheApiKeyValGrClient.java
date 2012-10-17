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
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
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
  private final static int SOME_RANDOM_INDEX        = END_INDEX - 20;
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
    for (int i = START_INDEX; i < MAX_NO_OF_ELEMENTS; i++) {
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
    clientIndex = waitForAllClientsToReachHere();
    if (clientIndex == 0) {
      destroyDs();
    }
    waitForAllClientsToReachHere();
    setEventualDs(toolkit, NAME_OF_DS);
    setKeyValGeneratorAndRunTest();

  }

  @Override
  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    log("Entering check is destroyed");
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        ToolkitCache tmpCache = toolkit.getCache("tempCache", String.class);
        Assert.assertFalse(tmpCache.isDestroyed());
        tmpCache.destroy();
        Assert.assertTrue(tmpCache.isDestroyed());
      }
      waitForAllClientsToReachHere();
      log("getting out of check is destroyed");
    } finally {
      clearDs();
    }
  }

  @Override
  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    log("Entering checkDestroy");
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        ToolkitCache tmpCache = super.toolkit.getCache("tempCache", String.class);
        Assert.assertFalse(tmpCache.isDestroyed());
        tmpCache.destroy();
        Assert.assertTrue(tmpCache.isDestroyed());
      }
      waitForAllClientsToReachHere();
      log("getting out of checkDestroyed");
    } finally {
      clearDs();
    }
  }

  private void destroyDs() {
    log("&&&&&&&&&&&&&&&&&&&DESTROYING CACHE&&&&&&&&&&&&&&");
    cache.destroy();
    log("Destroyed");
  }

  private void setKeyValGeneratorAndRunTest() throws Throwable {
    keyValueGenerator = new LiteralKeyLiteralValueGenerator();
    runTest();
    waitForAllClientsToReachHere();

    keyValueGenerator = new LiteralKeyNonLiteralValueGenerator();
    runTest();
    waitForAllClientsToReachHere();
  }


  private void runTest() throws Throwable {
    System.err
.println("#$#$#$#$#$#$#$#$#$#$#$#$$#$#$#$#$#$Calling Super.test^&*^&^*&^*&^*&^*&^*&^*&^*&^&*^&*^*^&*&");
    super.test();
    waitForAllClientsToReachHere();
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
    String methodName = "testRemoveListener";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        log("Client 0 adding listener");
        ToolkitCacheListener speciallListener = getSpecialListener();
        cache.addListener(speciallListener);
        log("Client 0 added istnere, now it will remove it");
        cache.removeListener(speciallListener);
        log("listener removed");
        log("now doing puts");
        putValues(START_INDEX, END_INDEX, methodName);
        log("DOne with puts");
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
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
    String methodName = "testAddListener";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      clientIndex = waitForAllClientsToReachHere();
      cache.setConfigField(ToolkitStoreConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME, MAX_NO_OF_ELEMENTS);
      if (clientIndex == 0) {
        log("client 0 adding listener tp cache");
        addListenerToCache();
        log("client 0 doing some puts");
        putValues(START_INDEX, END_INDEX, methodName);
        log("client 0 done putting");
        Assert.assertTrue(checkForAllKeysPut(START_INDEX, END_INDEX));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
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
    String methodName = "testUnpinAll";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        log("client 0 pinning keys");
        pinKeys(START_INDEX, END_INDEX);
        log("client 0 has pinned all keys");
        log("asserting that keys are pinned");
        Assert.assertTrue(checkKeysPinned(START_INDEX, END_INDEX));
        log("Unpinn keys");
        cache.unpinAll();
        log("Asserting that none of the keys is pinned");
        Assert.assertTrue(checkKeysNotPinned(START_INDEX, END_INDEX));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void testIsPinned() throws InterruptedException, BrokenBarrierException {
    String methodName = "testIsPinned";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        log("Client 0 checking that no key should be pinned rightnow!!");
        Assert.assertTrue(checkKeysNotPinned(START_INDEX, END_INDEX));
        log("no keys found pinned");
        log("Client 0 pinning keys from " + keyValueGenerator.getKey(START_INDEX) + " to "
            + keyValueGenerator.getKey(SOME_RANDOM_INDEX));
        pinKeys(START_INDEX, SOME_RANDOM_INDEX);
        log("Done pinning");
        log("asserting that keys from " + keyValueGenerator.getKey(START_INDEX) + " to excluding "
            + keyValueGenerator.getKey(SOME_RANDOM_INDEX) + " are actually pinned");
        Assert.assertTrue(checkKeysPinned(START_INDEX, SOME_RANDOM_INDEX));
        log("Asserting that keys not pinned are not actually pinned");
        Assert.assertTrue(checkKeysNotPinned(SOME_RANDOM_INDEX + 4, END_INDEX));
        cache.unpinAll();
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
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
    String methodName = "testSetPinned";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {

      if (clientIndex == 0) {
        for (int i = START_INDEX; i < END_INDEX; i++) {
          cache.setPinned(keyValueGenerator.getKey(i), true);
          cache.put(keyValueGenerator.getKey(i), keyValueGenerator.getValue(i));

          Assert.assertTrue(cache.isPinned(keyValueGenerator.getKey(i)));
        }
        int noOfElementsPut = END_INDEX - START_INDEX;
        Assert.assertEquals(noOfElementsPut, cache.size());

        for (int i = START_INDEX; i < END_INDEX; i++) {
          Assert.assertNotNull(cache.get(keyValueGenerator.getKey(i)));
          Assert.assertEquals(cache.get(keyValueGenerator.getKey(i)), keyValueGenerator.getValue(i));
        }
        }

      cache.unpinAll();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
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
    String methodName = "testPutIfAbsent";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        log("Client 0 doing putIfAbsent");
        cache.putIfAbsent(keyValueGenerator.getKey(KEY_ONE), keyValueGenerator.getValue(VALUE_ONE),
                          someRandomPastTime(), TWO_SECS, TWO_SECS);
        log("Client 0 Done with putIfAbsent");
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
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
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }



  private boolean checkMapShouldContainKeyValuePairs(Map tmap, int start, int someRandomIndex, int end) {
    /*
     * we had put in cache from start to someRandomIndex, so the tempMap should have a valid mapping for keys from start
     * to someRandomIndex and mapping should map to null for keys from someRandomIndex to end
     */
    for (int key = start; key < someRandomIndex; key++) {
      if (tmap.get(keyValueGenerator.getKey(key)) == null) {
        return false;
      }
    }
    for (int key = someRandomIndex; key < end; key++) {
      if (tmap.containsKey(keyValueGenerator.getKey(key)) && tmap.get(keyValueGenerator.getKey(key)) != null) { return false; }
    }
    return true;
  }

  private Boolean checkCacheShouldNotContainKeyValuePairs(ToolkitCache toolkitCache, Set setOfKeys) {
    for (Object key : setOfKeys) {
      toolkitCache.get(key);
      if (toolkitCache.get(key) == null) {
        log("Not found for : " + key + " value found is :" + toolkitCache.get(key));
        continue;
      }
      log("found entry for : " + key.toString() + " value = " + toolkitCache.get(key));
      return false;
    }
    return true;
  }

  private void testGetQuiet() throws Exception {
    String methodName = "testGetQuiet";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
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
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void testGetAllQuiet() throws Exception {
    String methodName = "testGetAllQuiet";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      if (clientIndex == 0) {
        log("client 0 will perform some puts");
        putValues(START_INDEX, SOME_RANDOM_INDEX, methodName);
        log("Client 0 done putting values");

        startTime = System.currentTimeMillis();
        endTime = startTime;
        log("Now Client 0 will perform getALlQuiet on cache for some time");
        while (timeElapsedInSecs() < MAX_TIME_TO_IDLE_IN_SECS - ONE_SEC) {
          tempMap = cache.getAllQuiet(getKeySet(START_INDEX, SOME_RANDOM_INDEX));
          Assert.assertTrue(checkMapShouldContainKeyValuePairs(tempMap, START_INDEX, SOME_RANDOM_INDEX, END_INDEX));
          endTime = System.currentTimeMillis();
        }
        log("CLient 0 done with doing getALLQuiet on cache");
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

          @Override
          public Boolean call() throws Exception {
            return checkCacheShouldNotContainKeyValuePairs(cache, getKeySet(SOME_RANDOM_INDEX, END_INDEX));
          }

        });
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void testPutNoReturn() throws Exception {
    String methodName = "testPutNoReturn";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);

    try {
      if (clientIndex == 0) {
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
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
      waitForAllClientsToReachHere();

    } finally {
      clearDs();
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
    super.toolkit = toolkit;
    barrier = toolkit.getBarrier("myBarrier", 2);
    map = chm = cache = toolkit.getCache(name, String.class);
    cache.setConfigField(ToolkitCacheConfigFields.MAX_TTI_SECONDS_FIELD_NAME, MAX_TIME_TO_IDLE_IN_SECS);
    store = (ToolkitStore) chm;
  }

  @Override
  public void setStrongDs(Toolkit toolkit, String name) {
    super.toolkit = toolkit;
    barrier = toolkit.getBarrier("myBarrier", 2);
    ToolkitCacheConfigBuilder configBuilder = new ToolkitCacheConfigBuilder().consistency(Consistency.STRONG);
    map = chm = cache = toolkit.getCache(name, configBuilder.build(), String.class);
    store = (ToolkitStore) chm;
  }
  @Override
  protected void putValues(int start, int end, String callingMethodName) {
    for (int iterator = start; iterator < end; iterator++) {
      keysPutUntilNow.add(keyValueGenerator.getKey(iterator));
      super.putValues(iterator, iterator + 1, callingMethodName);
    }
  }

}
