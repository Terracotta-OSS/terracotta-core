/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.junit.Assert;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public abstract class AbstractMapApiTestClientUtil extends ClientBase {
  private static final String   VALUES      = "values";
  private static final String   KEYS        = "keys";
  protected ToolkitBarrier      barrier;
  protected Map                 map;
  protected int                 clientIndex;
  protected KeyValueGenerator   keyValueGenerator;
  protected static final String NAME_OF_DS  = "myDS";
  protected final static int    START_INDEX = 0;
  protected final static int    MID_INDEX   = 25;
  protected final static int    END_INDEX   = 50;
  protected static final String STRONG      = "STRONG";
  protected static final String EVENTUAL    = "EVENTUAL";
  protected Toolkit             toolkit;

  public AbstractMapApiTestClientUtil(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit tk) throws Throwable {
    clearDs();
    waitForAllClientsToReachHere();
    checkGet();
    checkIsEmpty();
    checkClear();
    checkSize();
    checkContainsKey();
    checkRemoveOneArgs();
    checkPut();
    checkPutAll();
    checkKeySet();
    checkValues();
    checkIsDestroyed();
    checkDestroy();
    checkEntrySet();
  }

  private void checkEntrySet() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkEntrySet";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with client index = " + clientIndex);
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Set entrySet = map.entrySet();
      Assert.assertEquals(map.size(), entrySet.size());
      if (clientIndex == 1) {
        Assert.assertTrue(entrySetEqualsMap(map, entrySet));

        removeFromMap(START_INDEX, MID_INDEX);
        Assert.assertEquals(map.size(), entrySet.size());
        Assert.assertTrue(entrySetEqualsMap(map, entrySet));

      }
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        entrySet.removeAll(getArrayListForRange(START_INDEX, MID_INDEX, methodName));
        Assert.assertEquals(map.size(), entrySet.size());
        Assert.assertTrue(entrySetEqualsMap(map, entrySet));
        Assert.assertTrue(allKeyValuePairsArePresent(MID_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
    } finally {
      clearDs();
    }
  }

  private boolean entrySetEqualsMap(Map m, Set entrySet) {
    Iterator iterator = entrySet.iterator();
    while (iterator.hasNext()) {
      Map.Entry mapEntry = (Map.Entry) iterator.next();
      if (!m.containsKey(mapEntry.getKey())) {
        System.err.println("Key " + mapEntry.getKey() + "not present");
        return false;
      }
      if (!mapEntry.getValue().equals(m.get(mapEntry.getKey()))) {
        System.err.println("mismatch : mapentry : " + mapEntry.getValue() + " map.get : " + m.get(mapEntry.getKey())
                           + " for key :" + mapEntry.getKey());
        return false;
      }
    }
    return true;
  }

  private void checkValues() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkValuesCollection";
    clientIndex = waitForAllClientsToReachHere();
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Collection values = map.values();
      Assert.assertTrue(values.containsAll(getArrayListForRange(START_INDEX, END_INDEX, VALUES)));
      waitForAllClientsToReachHere();

      if (clientIndex == 1) {
        values.remove(keyValueGenerator.getValue(START_INDEX));
        Assert.assertFalse(values.contains(keyValueGenerator.getValue(START_INDEX)));
        Assert.assertFalse(map.containsKey(keyValueGenerator.getKey(START_INDEX)));
        try {
          values.removeAll(getArrayListForRange(START_INDEX, MID_INDEX, VALUES));
          if (map instanceof ToolkitMap || map instanceof ToolkitSortedMap) {
            // removeAll on collection returned by map.values() is not supported
            Assert.fail();
          } else {
            Assert.assertFalse(values.containsAll(getArrayListForRange(START_INDEX, MID_INDEX, VALUES)));
          }
        } catch (UnsupportedOperationException uoe) {
          log("verified that removeAll is no supported on collection returned by map.values");
        }

      }

      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, MID_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(values.containsAll(getArrayListForRange(START_INDEX, END_INDEX, VALUES)));
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        removeFromMap(START_INDEX, MID_INDEX);
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(values.containsAll(getArrayListForRange(START_INDEX, MID_INDEX, VALUES)));
      Assert.assertTrue(values.containsAll(getArrayListForRange(MID_INDEX, END_INDEX, VALUES)));
      log("Exiting " + methodName + " with ClientIndex = " + clientIndex);

    } finally {
      clearDs();
    }
  }

  private void checkKeySet() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkKeySet";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with client Index = " + clientIndex);
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Set keySet = map.keySet();
      Assert.assertTrue(keySet.containsAll(getArrayListForRange(START_INDEX, END_INDEX, KEYS)));
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        keySet.removeAll(getArrayListForRange(START_INDEX, MID_INDEX, KEYS));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(theseKeysInGivenRangeAreNotPresentInMap(START_INDEX, MID_INDEX, methodName));
      Assert.assertTrue(theseKeysInGivenRangeArePresentInMap(MID_INDEX, END_INDEX, methodName));
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, MID_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        removeFromMap(START_INDEX, MID_INDEX);
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(keySet.containsAll(getArrayListForRange(START_INDEX, MID_INDEX, KEYS)));
      Assert.assertTrue(keySet.containsAll(getArrayListForRange(MID_INDEX, END_INDEX, KEYS)));
      log("Exiting " + methodName + " with ClientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  protected void removeFromMap(int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      map.remove(keyValueGenerator.getKey(key));
    }
  }

  private List getArrayListForRange(int startIndex, int endIndex, String type) {
    List arrayList = new ArrayList();
    for (int key = startIndex; key < endIndex; key++) {
      if (type.equals(KEYS)) {
        arrayList.add(keyValueGenerator.getKey(key));
      } else if (type.equals(VALUES)) {
        arrayList.add(keyValueGenerator.getValue(key));
      }
    }
    return arrayList;
  }

  private void checkPutAll() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkPutAll";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        map.putAll(getMapWithKeyValuePairsInTheRange(START_INDEX, END_INDEX));
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertTrue(allKeyValuePairsArePresent(START_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private Map getMapWithKeyValuePairsInTheRange(int startIndex, int endIndex) {
    Map tempHashMap = new HashMap();
    for (int key = startIndex; key < endIndex; key++) {
      tempHashMap.put(keyValueGenerator.getKey(key), keyValueGenerator.getValue(key));
    }
    return tempHashMap;
  }

  private void checkPut() throws InterruptedException, BrokenBarrierException {
    // check that map returns null when we try to put a non-existent kay-value pair
    String methodName = "checkPut";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        Assert.assertTrue(putWorksForNonExistentKeys(START_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertTrue(putWorksForExistentKeys(START_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
    // check that map returns old value when we try to put a key-value pair where key is already existing
  }

  private boolean putWorksForExistentKeys(int startIndex, int endIndex, String methodName) {
    for (int key = startIndex; key < endIndex; key++) {
      Object oldValue = map.put(keyValueGenerator.getKey(key), keyValueGenerator.getValue(key));
      if (oldValue.equals(keyValueGenerator.getValue(key))) {
        continue;
      } else {
        log("map.put returned : " + oldValue + " for map.put(" + keyValueGenerator.getKey(key) + ","
            + keyValueGenerator.getValue(key) + ") : it was supposed to be null");
        return false;
      }

    }
    return true;

  }

  private boolean putWorksForNonExistentKeys(int startIndex, int endIndex, String methodName) {
    for (int key = startIndex; key < endIndex; key++) {
      Object oldValue = map.put(keyValueGenerator.getKey(key), keyValueGenerator.getValue(key));
      if (oldValue == null) {
        continue;
      } else {
        log("map.put returned : " + oldValue + " for map.put(" + keyValueGenerator.getKey(key) + ","
            + keyValueGenerator.getValue(key) + " : it was supposed to be null");
        return false;
      }

    }
    return true;

  }

  private void checkRemoveOneArgs() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkRemoveOneArgs";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      // if map doesn't contain the value,it returns null
      Assert.assertNull(map.remove(keyValueGenerator.getKey(START_INDEX)));
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertTrue(removeReturnsOldValue(START_INDEX, MID_INDEX));
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(theseKeysInGivenRangeAreNotPresentInMap(START_INDEX, MID_INDEX, methodName));
      Assert.assertTrue(theseKeysInGivenRangeArePresentInMap(MID_INDEX, END_INDEX, methodName));
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  protected boolean theseKeysInGivenRangeArePresentInMap(int startIndex, int endIndex, String callingMethodName) {
    log("in method " + callingMethodName + " Cheking that keys from " + keyValueGenerator.getKey(startIndex) + " to "
        + keyValueGenerator.getKey(endIndex) + " are present");
    for (int key = startIndex; key < endIndex; key++) {
      if (map.containsKey(keyValueGenerator.getKey(key))) {
        continue;
      } else {
        log("check map.remove failed beacuse : ");
        log(keyValueGenerator.getKey(key) + " not present , but it should be");
        return false;
      }
    }
    return true;
  }

  protected boolean theseKeysInGivenRangeAreNotPresentInMap(int startIndex, int endIndex, String callingMethodName) {
    log("in method " + callingMethodName + " Cheking that keys from " + keyValueGenerator.getKey(startIndex) + " to "
        + keyValueGenerator.getKey(endIndex) + " are not present");
    for (int key = startIndex; key < endIndex; key++) {
      if (!map.containsKey(keyValueGenerator.getKey(key))) {
        continue;
      } else {
        log("check map.remove failed beacuse : ");
        log(keyValueGenerator.getKey(key) + " was removed , but is still present");
        return false;
      }
    }
    return true;
  }

  private boolean removeReturnsOldValue(int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      Object removedValue = map.remove(keyValueGenerator.getKey(key));
      if (removedValue.equals(keyValueGenerator.getValue(key))) {
        continue;
      } else {
        log("check map.remove failed beacuse : ");
        log(removedValue + " not equal to " + keyValueGenerator.getValue(key));
        return false;
      }
    }
    return true;
  }

  private void checkContainsKey() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkContainsKey";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      // means map has no key at present
      Assert.assertTrue(map.isEmpty());
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Assert.assertTrue(mapContainsAllKeys(START_INDEX, END_INDEX, methodName));
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private boolean mapContainsAllKeys(int startIndex, int endIndex, String methodName) {
    log("entering mapContinasAllKeys on behalf of " + methodName);
    for (int key = startIndex; key < endIndex; key++) {
      if (map.containsKey(keyValueGenerator.getKey(key))) {
        continue;
      } else {
        log("map doesn't contain key : " + keyValueGenerator.getKey(key) + " failure!!");
        return false;
      }
    }
    log("exiting mapContinasAllKeys ");
    return true;
  }

  private void checkSize() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkSize";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      Assert.assertEquals("Failed in " + methodName, 0, map.size());
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals("Failed in " + methodName, END_INDEX - START_INDEX, map.size());
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void checkClear() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkClear";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals("Failed in " + methodName, END_INDEX - START_INDEX, map.size());
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        map.clear();
      }
      waitForAllClientsToReachHere();
      Assert.assertEquals("Failed in " + methodName, 0, map.size());
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void checkIsEmpty() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkIsEmpty";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      Assert.assertTrue(map.isEmpty());
      waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        map.put(keyValueGenerator.getKey(START_INDEX), keyValueGenerator.getValue(START_INDEX));
      }
      waitForAllClientsToReachHere();
      Assert.assertFalse(map.isEmpty());
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  private void checkGet() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkGet";
    Assert.assertNull("" + map.get(keyValueGenerator.getKey(START_INDEX)),
                      map.get(keyValueGenerator.getKey(START_INDEX)));
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      // nothing put in the map yet, so it must be empty and return null for every key we check
      log("*******Entering " + methodName + "with index = " + clientIndex);
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertTrue(allKeyValuePairsArePresent(START_INDEX, END_INDEX, methodName));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  protected boolean allKeyValuePairsArePresent(int startIndex, int endIndex, String callingMethodName) {
    log("Cheking that map contains all key value pairs from " + startIndex + " to " + endIndex + " on behalf of "
        + callingMethodName);
    for (int key = startIndex; key < endIndex; key++) {
      if (map.get(keyValueGenerator.getKey(key)).equals(keyValueGenerator.getValue(key))) {
        continue;
      } else {
        // key value pair not found in map
        log("failed in " + callingMethodName);
        log("map was supposed to contain " + keyValueGenerator.getValue(key) + " for key "
            + keyValueGenerator.getKey(key));
        log("whereas it contained: " + map.get(keyValueGenerator.getKey(key)));
        log("getting out of allKeyValuePairsArePresent abnormally!!");
        return false;
      }
    }
    log("getting out of allKeyValuePairsArePresent suceesfully");
    return true;
  }

  protected void putValues(int startIndex, int endIndex, String callingMethodName) {
    log("Putting values in map on behalf of " + callingMethodName);

    for (int key = startIndex; key < endIndex; key++) {
      map.put(keyValueGenerator.getKey(key), keyValueGenerator.getValue(key));
      log("Now Map contains " + map.get(keyValueGenerator.getKey(key)) + "for key : " + keyValueGenerator.getKey(key));
    }

    log("Done Putting values in map on behalf of " + callingMethodName);
  }

  protected void clearDs() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    if (clientIndex == 0) {
      map.clear();
    }
  }

  protected int waitForAllClientsToReachHere() throws InterruptedException, BrokenBarrierException {
    int localIndex = barrier.await();
    return localIndex;
  }

  protected void log(String info) {
    System.err.println("@@** - " + info);
  }

  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        ToolkitMap tmpMap = toolkit.getMap("tempMap", null, null);
        Assert.assertFalse(tmpMap.isDestroyed());
        tmpMap.destroy();
        Assert.assertTrue(tmpMap.isDestroyed());
      }
      barrier.await();
    } finally {
      clearDs();
    }
  }

  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    try {
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        ToolkitMap tmpMap = toolkit.getMap("tempMap", null, null);
        Assert.assertFalse(tmpMap.isDestroyed());
        tmpMap.destroy();
        Assert.assertTrue(tmpMap.isDestroyed());
      }
      barrier.await();
    } finally {
      clearDs();
    }
  }

  protected abstract void checkGetName() throws InterruptedException, BrokenBarrierException;

  public abstract void setDs(Toolkit toolkit, String name, String strongOrEventual);
}
