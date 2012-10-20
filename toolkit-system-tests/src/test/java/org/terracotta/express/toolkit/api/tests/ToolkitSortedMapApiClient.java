/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitSortedMap;

import com.tc.util.Assert;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class ToolkitSortedMapApiClient extends AbstractMapApiTestClientUtil {
  ToolkitSortedMap sortedMap;

  public ToolkitSortedMapApiClient(String[] args) {
    super(args);

  }

  @Override
  protected void test(Toolkit toolKit) throws Throwable {
    log("Started test for ToolkitSortedMap");
    setDsAndRunTest(toolKit);
  }

  private void setDsAndRunTest(Toolkit toolkit) throws Throwable {
    setDs(toolkit, NAME_OF_DS, null);
    log("Testing ToolkitMap with IntIntKeyValueGenerator");
    keyValueGenerator = new IntIntKeyValueGenerator();
    log("***Calling super.test***");
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with IntKeyNonLiteralValueGenerator");
    keyValueGenerator = new IntKeyNonLiteralValueGenerator();
    log("***Calling super.test***");
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with NonLiteralKeyNonLiteralValueGenerator");
    keyValueGenerator = new NonLiteralKeyNonLiteralValueGenerator();
    log("***Calling super.test***");
    super.test(toolkit);
    this.test();

    log("Testing ToolkitMap with NonLiteralKeyLiteralValueGenerator");
    keyValueGenerator = new NonLiteralKeyLiteralValueGenerator();
    log("***Calling super.test***");
    super.test(toolkit);
    this.test();

  }

  protected void test() throws Throwable {
    log("***This.Test started***");

    checkDestroy();
    checkIsDestroyed();
    checkGetName();
    checkFirstKey();
    checkLastKey();
    checkcontainsValue();
    checkHeadMap();
    checkSubMap();
    checkTailMap();
    log("***Test Passed***");

  }

  private void checkSubMap() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkSubMap";
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Map subMap = sortedMap.subMap(keyValueGenerator.getKey(START_INDEX), keyValueGenerator.getKey(MID_INDEX));
        Assert.assertTrue("subMap not created properly", subMap.size() > 0);
        Assert.assertTrue(sortedMapContains(subMap));
        removeFromMap(START_INDEX, MID_INDEX);
        Assert.assertTrue(mapShouldNotContainKeysValueInRange(subMap, START_INDEX, MID_INDEX));
        putValues(START_INDEX, MID_INDEX, methodName);
        Assert.assertTrue(mapShouldContainKeysValueInRange(subMap, START_INDEX, MID_INDEX));
        removeFromMap(subMap, START_INDEX, MID_INDEX, methodName);
        Assert.assertTrue(theseKeysInGivenRangeAreNotPresentInMap(START_INDEX, MID_INDEX, methodName));
        Assert.assertTrue(theseKeysInGivenRangeArePresentInMap(MID_INDEX, END_INDEX, methodName));
        try {
          subMap.put(keyValueGenerator.getKey(MID_INDEX + 2), keyValueGenerator.getValue(MID_INDEX + 2));
          Assert.fail();
        } catch (IllegalArgumentException iae) {
          log("Expected behaviour: couldn't put a value out of the range of headMap");
        }
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  private boolean mapShouldContainKeysValueInRange(Map tmpMap, int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      if (tmpMap.containsKey(keyValueGenerator.getKey(key))) {
        continue;
      } else {
        log("ERRor: headMap does not contain key: " + keyValueGenerator.getKey(key) + " it was  supposed to");
        return false;
      }
    }
    return true;
  }

  private void checkTailMap() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkTailMap";
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Map tailMap = sortedMap.tailMap(keyValueGenerator.getKey(MID_INDEX));
        Assert.assertTrue("tailMap not created properly", tailMap.size() > 0);
        Assert.assertTrue(sortedMapContains(tailMap));
        removeFromMap(MID_INDEX, END_INDEX);
        Assert.assertTrue(mapShouldNotContainKeysValueInRange(tailMap, MID_INDEX, END_INDEX));
        putValues(MID_INDEX, END_INDEX, methodName);
        removeFromMap(tailMap, MID_INDEX, END_INDEX, methodName);
        Assert.assertTrue(theseKeysInGivenRangeArePresentInMap(START_INDEX, MID_INDEX, methodName));
        Assert.assertTrue(theseKeysInGivenRangeAreNotPresentInMap(MID_INDEX, END_INDEX, methodName));
        try {
          tailMap.put(keyValueGenerator.getKey(MID_INDEX - 2), keyValueGenerator.getValue(MID_INDEX + 2));
          Assert.fail();
        } catch (IllegalArgumentException iae) {
          log("Expected behaviour: couldn't put a value out of the range of headMap");
        }
      }
      waitForAllClientsToReachHere();

      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  private void checkLastKey() throws InterruptedException, BrokenBarrierException {
    log("Entered checkLastKey method ");
    String methodName = "checkLastKey";
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        // as the last key put in map is END_INDEX -1 because we put from = START_INDEX to < END_INDEX
        Assert.assertEquals(keyValueGenerator.getKey(END_INDEX - 1), sortedMap.lastKey());
        sortedMap.remove(keyValueGenerator.getKey(END_INDEX - 1));
        Assert.assertEquals(keyValueGenerator.getKey(END_INDEX - 2), sortedMap.lastKey());
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  private void checkHeadMap() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkHeadMap";
    clientIndex = waitForAllClientsToReachHere();
    log("Entering " + methodName + " with clientIndex = " + clientIndex);
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Map headMap = sortedMap.headMap(keyValueGenerator.getKey(MID_INDEX));
        Assert.assertTrue("headMap not created properly", headMap.size() > 0);
        Assert.assertTrue(sortedMapContains(headMap));
        removeFromMap(START_INDEX, MID_INDEX);
        Assert.assertTrue(mapShouldNotContainKeysValueInRange(headMap, START_INDEX, MID_INDEX));
        putValues(START_INDEX, END_INDEX, methodName);
        removeFromMap(headMap, START_INDEX, MID_INDEX, methodName);
        Assert.assertTrue(theseKeysInGivenRangeAreNotPresentInMap(START_INDEX, MID_INDEX, methodName));
        Assert.assertTrue(theseKeysInGivenRangeArePresentInMap(MID_INDEX, END_INDEX, methodName));
        try {
          headMap.put(keyValueGenerator.getKey(MID_INDEX + 2), keyValueGenerator.getValue(MID_INDEX + 2));
          Assert.fail();
        } catch (IllegalArgumentException iae) {
          log("Expected behaviour: couldn't put a value out of the range of headMap");
        }
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " with clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  private void removeFromMap(Map tmpMap, int startIndex, int endIndex, String methodName) {
    for (int key = startIndex; key < endIndex; key++) {
      log("Removing " + keyValueGenerator.getKey(key) + " on behalf of " + methodName);
      tmpMap.remove(keyValueGenerator.getKey(key));
    }
  }

  private boolean mapShouldNotContainKeysValueInRange(Map headMap, int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      if (!headMap.containsKey(keyValueGenerator.getKey(key))) {
        continue;
      } else {
        log("ERRor: headMap contains key: " + keyValueGenerator.getKey(key) + " it was not supposed to");
        return false;
      }
    }
    return true;
  }

  private boolean sortedMapContains(Map headMap) {
    Set headMapset = headMap.entrySet();
    Set sortedMapSet = sortedMap.entrySet();
    if (sortedMapSet.containsAll(headMapset)) {
      return true;
    } else {
      return false;
    }
  }

  private void checkcontainsValue() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkcontainsValue";
    clientIndex = waitForAllClientsToReachHere();
    log("Entered " + methodName + " for clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertTrue(mapContainsAllValues(START_INDEX, END_INDEX));
        removeFromMap(START_INDEX, MID_INDEX);
        Assert.assertTrue(mapContainsAllValues(MID_INDEX, END_INDEX));
        Assert.assertTrue(mapDoesNotContainAllValues(START_INDEX, MID_INDEX));
      }
      waitForAllClientsToReachHere();
      log("Exiting " + methodName + " for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  private boolean mapDoesNotContainAllValues(int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      if (!sortedMap.containsValue(keyValueGenerator.getValue(key))) {
        continue;
      } else {
        log("Failed in mapDoesNotContainsAllValues ,because map contains value :" + keyValueGenerator.getValue(key));
        return false;
      }
    }
    return true;
  }

  private boolean mapContainsAllValues(int startIndex, int endIndex) {
    for (int key = startIndex; key < endIndex; key++) {
      if (sortedMap.containsValue(keyValueGenerator.getValue(key))) {
        continue;
      } else {
        log("Failed in mapContainsAllValues ,because map doesn't contain value :" + keyValueGenerator.getValue(key));
        return false;
      }
    }
    return true;
  }

  private void checkFirstKey() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkFirstKey";
    clientIndex = waitForAllClientsToReachHere();
    log("Entered " + methodName + " for clientIndex = " + clientIndex);
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        putValues(START_INDEX, END_INDEX, methodName);
      }
      waitForAllClientsToReachHere();
      if (clientIndex == 1) {
        Assert.assertEquals(keyValueGenerator.getKey(START_INDEX), sortedMap.firstKey());
        sortedMap.remove(keyValueGenerator.getKey(START_INDEX));
        Assert.assertEquals(keyValueGenerator.getKey(START_INDEX + 1), sortedMap.firstKey());
      }
      waitForAllClientsToReachHere();
      log("Exiting checkFirstKey for clientINdex = " + clientIndex);
    } finally {
      clearDs();
    }
  }

  @Override
  protected void checkGetName() throws InterruptedException, BrokenBarrierException {
    waitForAllClientsToReachHere();
    log("Entering checkGetName");
    try {
      Assert.assertEquals(NAME_OF_DS, sortedMap.getName());
      log("Exiting checkGetName");
    } finally {
      clearDs();
    }
  }

  @Override
  protected void checkIsDestroyed() throws InterruptedException, BrokenBarrierException {
    log("Entering check is destroyed");
    waitForAllClientsToReachHere();
    try {
      clientIndex = waitForAllClientsToReachHere();
      if (clientIndex == 0) {
        ToolkitSortedMap tmpSortedMap = toolkit.getSortedMap("tempSortedMap", String.class, String.class);
        Assert.assertFalse(tmpSortedMap.isDestroyed());
        tmpSortedMap.destroy();
        Assert.assertTrue(tmpSortedMap.isDestroyed());
      }
      waitForAllClientsToReachHere();
      log("getting out of check is destroyed for clientIndex = " + clientIndex);
    } finally {
      clearDs();
    }

  }

  @Override
  protected void checkDestroy() throws InterruptedException, BrokenBarrierException {
    String methodName = "checkDestroy";
    clientIndex = waitForAllClientsToReachHere();
    log("Entered " + methodName + " for clientIndex = " + clientIndex);
    try {
      if (clientIndex == 0) {
        ToolkitSortedMap tmpSortedMap = toolkit.getSortedMap("tempSortedMap", String.class, String.class);
        Assert.assertFalse(tmpSortedMap.isDestroyed());
        tmpSortedMap.destroy();
        Assert.assertTrue(tmpSortedMap.isDestroyed());
      }
      waitForAllClientsToReachHere();
      sortedMap.clear();
      log("getting out of checkDestroyed for clientIndex =" + clientIndex);
    } finally {
      clearDs();
    }
  }

  @Override
  public void setDs(Toolkit toolkit, String name, String strongOrEventual) {
    super.toolkit = toolkit;
    super.map = sortedMap = toolkit.getSortedMap(NAME_OF_DS, String.class, String.class);
    super.barrier = toolkit.getBarrier("myBarrier", getParticipantCount());
  }

}
