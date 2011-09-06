/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SetMapPerformanceTest extends TCTestCase {

  private static final int ADD_LENGTH = 200000;
  private static final int PUT_LENGTH = 200000;

  private TestResults      testResult;

  public void testSetPerformance() throws Exception {

    // These are here to make sure JIT compilation happens
    println("Warmup runs....");
    testResult = new TestResults(); // null
    Collection c = createSequencialData(2000);
    performTest(new ObjectIDSet(), c);
    performTest(new HashSet(), c);
    performTest(new ObjectIDSet(), c);
    performTest(new HashSet(), c);
    performTest(new ObjectIDSet(), c);
    performTest(new HashSet(), c);

    testResult = new TestResults();

    // real test begins - sequential
    c = createSequencialData(ADD_LENGTH);
    println();
    println("---------------------------------------------------------------------------------------");
    println("Sequential ObjectIDs");
    println("---------------------------------------------------------------------------------------");
    // performTest(new ObjectIDSet(), c);
    performTest(new ObjectIDSet(), c);
    performTest(new HashSet(), c);
    performTest(new THashSet(), c);

    c = null;
    testResult.printResults();
    testResult = new TestResults();

    // Random
    c = createRandomData(ADD_LENGTH);
    println();
    println("---------------------------------------------------------------------------------------");
    println("Random ObjectIDs");
    println("---------------------------------------------------------------------------------------");
    // performTest(new ObjectIDSet(), c);
    performTest(new ObjectIDSet(), c);
    performTest(new HashSet(), c);
    performTest(new THashSet(), c);

    testResult.printResults();
  }

  // This is no good. Need to improve this test 
  public void testTHashMapPerformance() {
    testResult = new TestResults();
    THashMap map = new THashMap();
    Set c = createSequencialData(PUT_LENGTH );
    
    performTest("Sequencial IDs" , map, c);

    c = null;
    map = new THashMap();
    c = createRandomData(PUT_LENGTH);
    performTest("Random IDs" , map, c);

    testResult.printResults();
  }

  private void performTest(String characteristics, Map map, Set ids) {
    TestRecord tr = testResult.getTestRecord("Map Performance Test : size = " + PUT_LENGTH);
    Object val = new Object();
    StopClock sc = new StopClock();
    sc.start();
    for (Iterator i = ids.iterator(); i.hasNext();) {
      map.put(i.next(), val);
    }
    sc.stop();
    tr.addResult( characteristics + " : " + map.getClass().getName()+ " : " + sc);
    
  }

  private void performTest(Set set, Collection c) {
    println("Running tests on " + set.getClass().getName());
    performAddsOneAtATime(set, c);
    set.clear();
    performAddAll(set, c);
    performRemovesOneAtATime(set, c);
    performRemoveAll(set, c);
  }

  private void performRemoveAll(Set set, Collection c) {
    TestRecord tr = testResult.getTestRecord("Performing removeAll() : size = " + set.size() + " removing " + c.size());
    StopClock sc = new StopClock();
    sc.start();
    set.removeAll(c);
    sc.stop();
    tr.addResult(set.getClass().getName() + " : " + sc);
  }

  private void performRemovesOneAtATime(Set set, Collection c) {
    TestRecord tr = testResult
        .getTestRecord("Performing remove() : size = " + set.size() + " removing " + c.size() / 2);
    StopClock sc = new StopClock();
    for (int j = 0; j < 2; j++) {
      int count = 0;
      for (Iterator i = c.iterator(); i.hasNext() && count++ <= c.size() / 4;) {
        sc.start();
        set.remove(i.next());
        sc.stop();
      }
    }
    tr.addResult(set.getClass().getName() + " : " + sc);
  }

  private static void println() {
    println("");
  }

  private static void println(String s) {
    System.out.println(s);
  }

  private void performAddAll(Set set, Collection c) {
    TestRecord tr = testResult.getTestRecord("Performing addAll() : size = " + c.size());
    StopClock sc = new StopClock();
    sc.start();
    set.addAll(c);
    sc.stop();
    tr.addResult(set.getClass().getName() + " : " + sc);
  }

  private Set createSequencialData(int size) {
    return createSequencialData(0, size);
  }

  private Set createSequencialData(int init, int size) {
    return createSequencialData(init, size, 1);
  }

  private Set createSequencialData(int init, int size, int step) {
    HashSet set = new LinkedHashSet();
    while (size-- > 0) {
      set.add(new ObjectID(init));
      init += step;
    }
    return set;
  }

  Random r = new Random();

  private Set createRandomData(int size) {
    HashSet set = new LinkedHashSet();
    for (int j = 0; j < size; j++) {
      set.add(new ObjectID(r.nextLong()));
    }
    return set;
  }

  private void performAddsOneAtATime(Set set, Collection c) {
    TestRecord tr = testResult.getTestRecord("Performing add() : size = " + c.size());
    StopClock sc = new StopClock();
    for (Iterator i = c.iterator(); i.hasNext();) {
      sc.start();
      set.add(i.next());
      sc.stop();
    }
    tr.addResult(set.getClass().getName() + " : " + sc);
  }

  static class StopClock {
    long cumulative;
    long start;
    long lastLap;

    void start() {
      start = System.currentTimeMillis();
    }

    void stop() {
      lastLap = System.currentTimeMillis() - start;
      cumulative += lastLap;
    }

    void reset() {
      cumulative = start = lastLap = 0;
    }

    public String dump() {
      return "Time taken = " + cumulative + " ms  : Last Lap = " + lastLap + " ms";
    }

    public String toString() {
      return "Time taken = " + cumulative + " ms";
    }
  }

  static class TestResults {
    Map tests = new HashMap();

    public TestRecord getTestRecord(String testName) {
      TestRecord tr = (TestRecord) tests.get(testName);
      if (tr == null) {
        tr = new TestRecord(testName);
        tests.put(testName, tr);
      }
      return tr;
    }

    public void printResults() {
      println();
      println("---------------------------------------------------------------------------------------");
      println(" TEST RESULTS");
      println("---------------------------------------------------------------------------------------");
      println();
      for (Iterator i = tests.values().iterator(); i.hasNext();) {
        TestRecord tr = (TestRecord) i.next();
        tr.printResults();
      }
    }

  }

  static class TestRecord {

    private final String testName;
    private final List   results = new ArrayList();

    public TestRecord(String testName) {
      this.testName = testName;
    }

    public void printResults() {
      println();
      println(testName);
      println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      for (Iterator i = results.iterator(); i.hasNext();) {
        println(String.valueOf(i.next()));
      }
    }

    public void addResult(String result) {
      results.add(result);
    }

  }
}
