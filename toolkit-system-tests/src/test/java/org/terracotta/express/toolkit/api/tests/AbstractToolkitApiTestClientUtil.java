/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.junit.Assert;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractToolkitApiTestClientUtil extends ClientBase {
  ToolkitBarrier    barrier;
  ConcurrentMap     map;
  int               index;
  final int         START = 0;
  final int         END   = 50;
  KeyValueGenerator keyValueGenerator;
  protected Map     tempMap;

  public AbstractToolkitApiTestClientUtil(String[] args) {
    super(args);
  }

  public abstract void setDs(Toolkit toolkit);

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    checkGet();
    checkIsEmpty();
    checkClear();
    checkContainsKey();
    checkRemoveTwoArgs();
    checkRemoveThreeArgs();
    checkReplaceTwoARgs();
    checkReplaceThreeArgs();
    checkPut();
    checkPutIfAbsent();
    checkSize();
    checkPutALL();
    checkKeySet();
    checkValues();
    checkEntrySet();
  }

  private void checkReplaceTwoARgs() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        map.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertEquals(map.get(keyValueGenerator.getKey(1)), keyValueGenerator.getValue(1));
      index = barrier.await();
      if (index == 0) {
        map.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }

      barrier.await();
      Assert.assertFalse(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertTrue(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));

    } finally {
      tearDown();
    }
  }

  private void checkReplaceThreeArgs() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        map.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertEquals(map.get(keyValueGenerator.getKey(1)), keyValueGenerator.getValue(1));
      index = barrier.await();
      if (index == 0) {
        map.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2), keyValueGenerator.getValue(3));
      }
      barrier.await();
      Assert.assertTrue(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertFalse(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));
      index = barrier.await();
      if (index == 0) {
        map.replace(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1), keyValueGenerator.getValue(2));
      }
      barrier.await();
      Assert.assertFalse(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(1)));
      Assert.assertTrue(map.get(keyValueGenerator.getKey(1)).equals(keyValueGenerator.getValue(2)));

    } finally {
      tearDown();
    }
  }

  private void checkRemoveThreeArgs() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 1) {
        map.put(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(1)));
      index = barrier.await();
      if (index == 1) {
        map.remove(keyValueGenerator.getKey(1), keyValueGenerator.getValue(2));
      }
      barrier.await();
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(1)));
      index = barrier.await();
      if (index == 1) {
        map.remove(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
      }
      barrier.await();
      Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(1)));
    } finally {
      tearDown();
    }
  }

  private void checkPutIfAbsent() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(1)));
      index = barrier.await();
      System.err.println("****In Method checkPutIfAbsent****");
      System.err.println("******index = " + index + "******");
      if (index == 1) {
        map.putIfAbsent(keyValueGenerator.getKey(1), keyValueGenerator.getValue(1));
        System.err.println("Key" + keyValueGenerator.getKey(1) + " " + keyValueGenerator.getValue(1) + " Value");
        System.err.println("just after put " + map.get(keyValueGenerator.getKey(1)));
        System.err.println("Put done");
      }
      barrier.await();
      System.err.println(map.get(keyValueGenerator.getKey(1)));
      Assert.assertEquals(keyValueGenerator.getValue(1), map.get(keyValueGenerator.getKey(1)));
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(1)));
      index = barrier.await();
      if (index == 0) {
        map.putIfAbsent(keyValueGenerator.getKey(1), keyValueGenerator.getKey(2));
      }
      barrier.await();
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(1)));
      Assert.assertNotSame(keyValueGenerator.getValue(2), map.get(keyValueGenerator.getKey(1)));

    } finally {
      tearDown();
    }
  }

  protected abstract void checkGetName() throws InterruptedException, BrokenBarrierException;

  protected abstract void checkIsDestroyed() throws InterruptedException, BrokenBarrierException;

  protected abstract void checkDestroy() throws InterruptedException, BrokenBarrierException;

  private void checkEntrySet() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Set entrySet = map.entrySet();
      Assert.assertEquals(entrySet.size(), map.size());
      Assert.assertTrue(checkEntrySetEqualsMap(entrySet));

      index = barrier.await();
      if (index == 0) {
        map.remove(keyValueGenerator.getKey(10));
      }
      barrier.await();
      Assert.assertFalse(entrySet.contains(keyValueGenerator.getKey(10)));

      index = barrier.await();
      if (index == 0) {
        Iterator iterator = entrySet.iterator();
        while (iterator.hasNext()) {
          Map.Entry mapEntry = (Map.Entry) iterator.next();
          if ((mapEntry.getKey()).equals(keyValueGenerator.getKey(20))) {
            iterator.remove();
            break;

          }
        }
      }
        barrier.await();
        Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(20)));
        barrier.await();
      
    } finally {
      tearDown();
    }
  }

  private boolean checkEntrySetEqualsMap(Set entrySet) {
    Iterator iterator = entrySet.iterator();
    while (iterator.hasNext()) {
      Map.Entry mapEntry = (Map.Entry) iterator.next();
      if (!map.containsKey(mapEntry.getKey())) return false;
      if (mapEntry.getValue() != map.get(mapEntry.getKey())) return false;
    }
    return true;
  }

  private void checkValues() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 1) {
        doSomePuts(START, END);
      }
      barrier.await();
      Collection valuesCollection = map.values();
      index = barrier.await();
      if (index == 1) {
        map.remove(keyValueGenerator.getKey(20));
      }
      barrier.await();
      Assert.assertFalse(valuesCollection.contains(keyValueGenerator.getKey(20)));

      index = barrier.await();
      if (index == 1) {
        valuesCollection.remove(keyValueGenerator.getKey(30));
      }
      barrier.await();

    } catch (UnsupportedOperationException uoe) {
      System.err.println("map.values().collection is not a supported operation");
    } finally {

      tearDown();
    }
  }

  private void checkKeySet() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      ArrayList arrayList = getNewArrayList(START, END);
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Set keySet = map.keySet();
      Assert.assertTrue(keySet.containsAll(arrayList));
      Assert.assertTrue(keySet.contains(keyValueGenerator.getKey(20)));
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(20)));

      index = barrier.await();
      if (index == 0) {
        map.remove(keyValueGenerator.getKey(20));
      }
      barrier.await();

      Assert.assertFalse(keySet.contains(keyValueGenerator.getKey(20)));
      Assert.assertFalse(keySet.containsAll(arrayList));

      barrier.await();
      arrayList.remove(keyValueGenerator.getKey(20));
      Assert.assertTrue(keySet.containsAll(arrayList));
      Assert.assertTrue(map.containsKey(keyValueGenerator.getKey(30)));
      index = barrier.await();
      if (index == 0) {
        keySet.remove(keyValueGenerator.getKey(30));
      }
      barrier.await();
      Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(30)));
    } finally {
      tearDown();
    }
  }

  private ArrayList getNewArrayList(int start, int end) {
    ArrayList arrayList = new ArrayList();
    for (int i = start; i < end; i++) {
      arrayList.add(keyValueGenerator.getKey(i));
    }
    return arrayList;
  }

  private void checkPutALL() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {

      tempMap = new HashMap();
      Assert.assertTrue(map.isEmpty());
      index = barrier.await();
      if (index == 0) {
        doSomePutsInTempMap(START, END);
        map.putAll(tempMap);
      }
      barrier.await();
      Assert.assertEquals(map.size(), END - START);
      Assert.assertTrue(checkKeyValuePairs(START, END));
      barrier.await();
    } finally {
      tearDown();
    }
  }

  void doSomePutsInTempMap(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      tempMap.put(keyValueGenerator.getKey(iterator), keyValueGenerator.getValue(iterator));
    }
  }

  private void checkSize() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertTrue("Map Should've Been Empty", map.size() == 0);
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertEquals(map.size(), END - START);
    } finally {
      tearDown();
    }

  }

  private void checkRemoveTwoArgs() throws InterruptedException, BrokenBarrierException {
    final int REMOVE_COUNT = 20;
    final int REMOVE_START_INDEX = START + REMOVE_COUNT;

    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertTrue("checkContainsKey Failed", checkAllKeysPresent(START, END));
      index = barrier.await();
      if (index == 0) {
        doSomeRemoves(REMOVE_START_INDEX, REMOVE_COUNT);
      }
      barrier.await();
      Assert.assertTrue("checkContainsKey Failed", checkAllKeysPresent(START, REMOVE_START_INDEX));
      Assert.assertTrue("checkContainsKey Failed", checkAllKeysAbsent(REMOVE_START_INDEX, REMOVE_COUNT));
      Assert.assertTrue("checkContainsKey Failed", checkAllKeysPresent(REMOVE_START_INDEX + REMOVE_COUNT, END));

      index = barrier.await();
      if (index == 0) {
        Object value = map.remove(keyValueGenerator.getKey(REMOVE_START_INDEX - 1));
        Assert.assertEquals(value.toString(), keyValueGenerator.getValue(REMOVE_START_INDEX - 1).toString());
      }
      barrier.await();

    } finally {
      tearDown();
    }
  }

  private boolean checkAllKeysAbsent(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      if (map.containsKey(keyValueGenerator.getKey(iterator))) {
        return false;
      } else {
        continue;
      }
    }
    return true;
  }

  private void doSomeRemoves(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      map.remove(keyValueGenerator.getKey(iterator));
    }
  }

  private void checkContainsKey() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(0)));
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertTrue("checkContainsKey Failed", checkAllKeysPresent(START, END));

    } finally {
      tearDown();
    }
  }

  private boolean checkAllKeysPresent(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      if (map.containsKey(keyValueGenerator.getKey(iterator))) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  private void checkClear() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertTrue(map.isEmpty());
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertFalse(map.isEmpty());
      index = barrier.await();
      if (index == 0) {
        map.clear();
      }
      barrier.await();
      Assert.assertTrue(map.isEmpty());
    } finally {
      tearDown();
    }
  }

  private void checkPut() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertTrue("CheckPut Failed, map is not empty", map.isEmpty());
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertFalse("CheckPut Failed, map is empty", map.isEmpty());
      Assert.assertTrue(checkKeyValuePairs(START, END));
      Object value = null;
      Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(101)));
      index = barrier.await();
      if (index == 0) {
        value = map.put(keyValueGenerator.getKey(101), keyValueGenerator.getValue(101));
      }
      barrier.await();
      Assert.assertEquals(keyValueGenerator.getValue(101), map.get(keyValueGenerator.getKey(101)));
      Assert.assertNull(value);
      index = barrier.await();
      if (index == 0) {
        value = map.put(keyValueGenerator.getKey(101), keyValueGenerator.getValue(201));
        Assert.assertNotNull(value);
        Assert.assertEquals(value.toString(), keyValueGenerator.getValue(101).toString());
      }
      barrier.await();
      Assert.assertEquals(keyValueGenerator.getValue(201), map.get(keyValueGenerator.getKey(101)));
    } finally {
      tearDown();
    }

  }

  private void checkIsEmpty() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertTrue(map.isEmpty());
      index = barrier.await();
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertFalse(map.isEmpty());
    } finally {
      tearDown();
    }
  }

  protected void tearDown() throws InterruptedException, BrokenBarrierException {
    barrier.await();
    map.clear();
  }

  protected void setUp() throws InterruptedException, BrokenBarrierException {
    barrier.await();
  }

  private void checkGet() throws InterruptedException, BrokenBarrierException {
    int key = 0;
    setUp();
    try {
      System.err.println("in checkGet() " + map.get(keyValueGenerator.getKey(key)));
      Assert.assertNull(map.get(keyValueGenerator.getKey(key)));
      index = barrier.await();
      System.err.println("*****In checkGet Method*****");
      System.err.println("****Index = " + index + "****");
      if (index == 0) {
        doSomePuts(START, END);
      }
      barrier.await();
      Assert.assertTrue(checkKeyValuePairs(START, END));
      barrier.await();
    } finally {
      tearDown();
    }
  }

  protected boolean checkKeyValuePairs(int start, int end) {
    Assert.assertNotNull("map is null", map);
    Assert.assertNotNull("keyValueGenerator null", keyValueGenerator);
    System.err.print("Entered checkKeyValuePairs");
    for (int iterator = start; iterator < end; iterator++) {
      if (map.get(keyValueGenerator.getKey(iterator)).equals(keyValueGenerator.getValue(iterator))) {
        System.err.println("Key exists : " + keyValueGenerator.getKey(iterator));
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  protected void doSomePuts(int start, int end) {
    for (int iterator = start; iterator < end; iterator++) {
      map.put(keyValueGenerator.getKey(iterator), keyValueGenerator.getValue(iterator));
    }
  }

}
