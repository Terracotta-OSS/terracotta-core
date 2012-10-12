package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.toolkit.api.tests.MyInt;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

public class ToolkitMapApiTestClient extends ClientBase {
  private ToolkitMap     map;
  private ToolkitBarrier barrier;
  private int            index;
  private Map            tmpMap;
  private Toolkit        toolKit;

  public ToolkitMapApiTestClient(String[] args) {
    super(args);

  }

  @Override
  protected void test(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
    map = toolkit.getMap("myMap", null, null);
    barrier = toolkit.getBarrier("myBarrier", 2);
    this.toolKit = toolkit;

    checkIsEmpty();
    checkPutIfAbsent();
    checkPut();
    checkGet();
    checkRemove();
    checkSize();
    checkContainsKey();
    checkContainsValues();
    checkClear();
    checkPutALL();
    checkKeySet();
    checkValues();
    checkEntrySet();
    checkGetName();
    checkIsDestroyed(toolKit);
    checkDestroy(toolKit);
    checkGetLock();

  }

  private void checkGetLock() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {

      index = barrier.await();
      final ToolkitReadWriteLock rwLock = map.getReadWriteLock();
      index = barrier.await();
      if (index == 0) doSomeLiteralLiteralPuts(0, 100);
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      index = barrier.await();
      final AtomicReference<Throwable> atomicRef = new AtomicReference();
      Thread thread1 = new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            System.out.println("THread1 running");
            cyclicBarrier.await();
            System.out.println("Acquring Lock");
            rwLock.writeLock().lock();
            try {
              cyclicBarrier.await();
              System.out.println("Waiting for 5 secs,Holding the lock");
              Thread.sleep(50000);

            } finally {
              rwLock.writeLock().unlock();
              System.out.println("Unlocked");
            }

          } catch (Throwable e) {
            atomicRef.set(e);
            e.printStackTrace();
            System.out.println("THread1 complete");
          }

        }
      }

      );

      Thread thread2 = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            System.out.println("Starting Thread 2");
            cyclicBarrier.await();
            cyclicBarrier.await();
            long now = System.currentTimeMillis();
            System.out.println("THread2 Attempting to get ");
            map.get(20);
            System.out.println("THread2 got the value");
            long diff = System.currentTimeMillis() - now;

            Assert.assertTrue("GetLock not working Properly " + diff + "MilliSecs", diff >= 40000);
            System.out.println("THread2 complete");
          } catch (Exception e) {
            System.out.println("THread2 complete,In catch");
            atomicRef.set(e);

          }

        }
      }

      );
      index = barrier.await();
      if (index == 0) {
        System.out.println("Starting Threads");
        thread1.start();
        thread2.start();
        System.out.println("Threads Started");
        thread1.join();
        thread2.join();
        System.out.println("Joined");
      }
      barrier.await();
      Assert.assertNull("Some problem", atomicRef.get());

    } finally {
      System.out.println("Tearing Down");
      tearDown();
    }
  }

  private void checkIsDestroyed(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        ToolkitMap toolkitMap = toolkit.getMap("myMap", null, null);
        Assert.assertFalse(toolkitMap.isDestroyed());
        toolkitMap.destroy();
        Assert.assertTrue(toolkitMap.isDestroyed());
      }
      barrier.await();
      map = toolkit.getMap("myMap", null, null);
    } finally {
      tearDown();
    }
  }

  private void checkDestroy(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) {
        ToolkitMap toolkitMap = toolkit.getMap("myMap", null, null);
        Assert.assertFalse(toolkitMap.isDestroyed());
        toolkitMap.destroy();
        Assert.assertTrue(toolkitMap.isDestroyed());
      }
      barrier.await();
      map = toolkit.getMap("myMap", null, null);
    } finally {
      tearDown();
    }
  }

  private void checkGetName() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertEquals("myMap", map.getName());
    } finally {
      tearDown();
    }

  }

  private void checkEntrySet() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      index = barrier.await();
      if (index == 0) doSomeLiteralLiteralPuts(0, 100);
      barrier.await();
      Set entrySet = map.entrySet();

      Assert.assertEquals(entrySet.size(), map.size());
      Assert.assertTrue(chkEntrySetEqualsMap(entrySet));

      index = barrier.await();
      if (index == 0) map.remove(10);
      barrier.await();
      Assert.assertFalse(entrySet.contains(10));

      index = barrier.await();
      if (index == 0) {
        Iterator iterator = entrySet.iterator();
        while (iterator.hasNext()) {
          Map.Entry mapEntry = (Map.Entry) iterator.next();
          if ((Integer) (mapEntry.getKey()) == 20) iterator.remove();
        }
      }
      barrier.await();
      Assert.assertFalse(map.containsKey(20));

      barrier.await();
    } finally {
      tearDown();
    }
  }

  private boolean chkEntrySetEqualsMap(Set entrySet) {
    Iterator iterator = entrySet.iterator();
    while (iterator.hasNext()) {
      Map.Entry mapEntry = (Map.Entry) iterator.next();
      if (!map.containsKey(mapEntry.getKey())) return false;
      if (mapEntry.getValue() != map.get(mapEntry.getKey())) return false;
    }
    return true;
  }

  private void checkValues() throws InterruptedException, BrokenBarrierException {
    Boolean exceptionOccured = false;
    setUp();
    try {
      index = barrier.await();
      if (index == 0) doSomeLiteralLiteralPuts(0, 100);
      barrier.await();

      ArrayList arrayList = getNewArrayList(0, 100);
      Collection valuesCollection = map.values();
      Assert.assertTrue(valuesCollection.containsAll(arrayList));

      Assert.assertTrue(map.containsValue(20));
      Assert.assertTrue(valuesCollection.contains(20));
      index = barrier.await();
      if (index == 0) map.remove(20);
      barrier.await();
      Assert.assertFalse(map.containsValue(20));
      Assert.assertFalse(valuesCollection.contains(20));

      Assert.assertTrue(map.containsValue(30));
      Assert.assertTrue(valuesCollection.contains(30));
      valuesCollection.remove(30);
      Assert.fail("remove not supported currently in map.values() collection"
                  + "Control won't reach here,This line Should not get printed");
      barrier.await();
      Assert.assertFalse(map.containsValue(30));
      Assert.assertFalse(valuesCollection.contains(30));

      Assert.assertTrue(exceptionOccured);

      Assert.assertFalse(map.containsValue(30));
      Assert.assertFalse(valuesCollection.contains(30));

    } catch (UnsupportedOperationException uop) {

      exceptionOccured = true;
    } finally {
      tearDown();
    }

  }

  private void checkKeySet() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {

      index = barrier.await();
      ArrayList arrayList = getNewArrayList(0, 100);
      if (index == 0) doSomeLiteralLiteralPuts(0, 100);
      barrier.await();
      Set keySet = map.keySet();
      Assert.assertTrue(keySet.containsAll(arrayList));
      Assert.assertTrue(keySet.contains(20));
      Assert.assertTrue(map.containsKey(20));

      index = barrier.await();
      if (index == 0) map.remove(20);
      barrier.await();

      Assert.assertFalse(keySet.contains(20));
      Assert.assertFalse(keySet.containsAll(arrayList));

      barrier.await();
      arrayList.remove(20);
      Assert.assertTrue(keySet.containsAll(arrayList));
      Assert.assertTrue(map.containsKey(30));
      index = barrier.await();
      if (index == 0) keySet.remove(30);
      barrier.await();
      Assert.assertFalse(map.containsKey(30));

    } finally {
      tearDown();
    }
  }

  private ArrayList getNewArrayList(int start, int count) {
    ArrayList arrayList = new ArrayList();
    for (int i = start; i < start + count; i++) {
      arrayList.add(i);
    }
    return arrayList;
  }

  private void checkPutALL() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      int start, count;
      tmpMap = new HashMap();
      start = 0;
      count = 100;
      somePutsinTmpMap(start, count);
      map.putAll(tmpMap);
      Assert.assertTrue(chkAllLiteralValues(10, 10));
      Assert.assertTrue(chkAllLiteralKeys(start, count));
    } finally {
      tearDown();
    }
  }

  private void somePutsinTmpMap(int start, int count) {
    for (int itrtr = start; itrtr < start + count; itrtr++) {
      tmpMap.put(itrtr, itrtr);
    }

  }

  private void checkClear() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertEquals(0, map.size());
      this.index = barrier.await();
      if (index == 0) doSomeLiteralLiteralPuts(0, 100);
      barrier.await();
      Assert.assertEquals(100, map.size());
      this.index = barrier.await();
      if (index == 0) map.clear();
      barrier.await();
      Assert.assertEquals(0, map.size());
    } finally {
      tearDown();
    }

  }

  private void checkRemove() throws InterruptedException, BrokenBarrierException {

    this.index = barrier.await();
    if (index == 0) map.put("key1", "value1");
    barrier.await();
    Assert.assertTrue(map.containsKey("key1"));
    this.index = barrier.await();
    if (index == 0) map.remove("key1");
    barrier.await();
    Assert.assertFalse(map.containsKey("key1"));
  }

  private void checkGet() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      this.index = barrier.await();
      if (index == 0) map.put("key1", "value1");
      barrier.await();
      Assert.assertNotNull(map.get("key1"));
      Assert.assertNull(map.get("keyThatWasNotPut"));
    } finally {
      tearDown();
    }
  }

  private void checkPut() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      this.index = barrier.await();
      if (index == 0) map.put("key1", "value1");
      barrier.await();
      Assert.assertTrue(map.containsValue("value1"));
      this.index = barrier.await();
      if (index == 0) map.put("key1", "value2");
      barrier.await();
      Assert.assertTrue(map.containsValue("value2"));
      Assert.assertFalse(map.containsValue("value1"));

    } finally {
      tearDown();
    }
  }

  private void checkPutIfAbsent() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      this.index = barrier.await();
      if (index == 0) {
        map.putIfAbsent("key1", "value1");
      }
      barrier.await();
      Assert.assertEquals("value1", map.get("key1"));
      if (index == 0) {
        map.putIfAbsent("key1", "value2");
      }
      barrier.await();
      Assert.assertEquals("value1", map.get("key1"));
    } finally {
      tearDown();
    }
  }

  private void tearDown() throws InterruptedException, BrokenBarrierException {
    barrier.await();
    map.clear();
  }

  private void setUp() throws InterruptedException, BrokenBarrierException {
    barrier.await();
  }

  private void checkContainsValues() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      // first put and ten check if those keys to exist
      this.index = barrier.await();
      if (this.index == 0) doSomeLiteralLiteralPuts(200, 10);
      barrier.await();
      chkAllLiteralValues(200, 10);

      this.index = barrier.await();
      if (this.index == 0) doSomeNonLiteralNonLiteralPuts(300, 10);
      barrier.await();
      chkAllNonLiteralValues(300, 10);

      this.index = barrier.await();
      if (this.index == 0) doSomeLiteralNonLiteralPuts(400, 10);
      barrier.await();
      chkAllNonLiteralValues(400, 10);
      // check for String type keys and negative number
      this.index = barrier.await();
      if (this.index == 0) {
        map.put("key1", "key1");
        map.put("key1", -1);
      }
      Assert.assertFalse(map.containsValue("key1"));
      Assert.assertTrue(map.containsValue(-1));
      barrier.await();

      // now chk for the keys that we didn't put
      Assert.assertFalse(map.containsKey(160));
      Assert.assertFalse(map.containsKey(new MyInt(160)));

    } finally {
      tearDown();
    }
  }

  private boolean chkAllLiteralValues(int start, int count) {

    for (int i = start; i < start + count; i++) {
      Boolean contains = map.containsValue(i);
      if (contains == false) {
        return false;
      } else continue;
    }

    return true;
  }

  private boolean chkAllNonLiteralValues(int start, int count) {

    for (int i = start; i < start + count; i++) {
      Boolean contains = map.containsValue(new MyInt(i));
      if (contains == false) {
        return false;
      } else continue;
    }

    return true;
  }

  private void checkContainsKey() throws InterruptedException, BrokenBarrierException {
    setUp();

    try {
      // first put and ten check if those keys to exist
      this.index = barrier.await();
      if (this.index == 0) doSomeLiteralLiteralPuts(200, 10);
      barrier.await();
      Assert.assertTrue(chkAllLiteralKeys(200, 10));

      this.index = barrier.await();
      if (this.index == 0) doSomeNonLiteralNonLiteralPuts(300, 10);
      barrier.await();
      Assert.assertTrue(chkAllNonLiteralKeys(300, 10));

      this.index = barrier.await();
      if (this.index == 0) doSomeLiteralNonLiteralPuts(400, 10);
      barrier.await();
      Assert.assertTrue(chkAllLiteralKeys(400, 10));
      // check for String type keys and negative number
      this.index = barrier.await();

      map.put("key1", "key1");

      Assert.assertTrue(map.containsValue("key1"));
      map.put("key2", -1);

      Assert.assertTrue(map.containsKey("key2"));
      map.put(-1, -1);
      Assert.assertTrue(map.containsKey(-1));

      barrier.await();
      // now chk for the keys that we didn't put
      Assert.assertFalse(map.containsKey(160));
      Assert.assertFalse(map.containsKey(new MyInt(160)));
    } finally {
      tearDown();
    }
  }

  private boolean chkAllLiteralKeys(int start, int count) {
    for (int i = start; i < start + count; i++) {
      Boolean contains = map.containsKey(i);
      if (contains == false) {
        return false;
      } else continue;
    }

    return true;
  }

  private boolean chkAllNonLiteralKeys(int start, int count) {
    for (int i = start; i < start + count; i++) {
      Boolean contains = map.containsKey(new MyInt(i));
      if (contains == false) {
        return false;
      } else continue;
    }

    return true;
  }

  private void checkIsEmpty() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      Assert.assertTrue(map.isEmpty());
      this.index = barrier.await();
      if (index == 0) map.put(1.0, "value1");
      Assert.assertFalse(map.isEmpty());
    } finally {
      tearDown();
    }
  }

  private void checkSize() throws InterruptedException, BrokenBarrierException {
    setUp();
    try {
      checkSize(0);
      this.index = barrier.await();
      if (index == 0) doSomeLiteralLiteralPuts(0, 50);
      barrier.await();
      checkSize(50);

      this.index = barrier.await();
      if (index == 0) doSomeNonLiteralNonLiteralPuts(51, 50);
      barrier.await();
      checkSize(100);

      this.index = barrier.await();
      if (index == 0) doSomeLiteralNonLiteralPuts(101, 50);
      barrier.await();
      checkSize(150);
      barrier.await();
    } finally {
      tearDown();
    }
  }

  private void checkSize(int size) {
    Assert.assertEquals(size, map.size());
  }

  private void doSomeNonLiteralNonLiteralPuts(int start, int count) {
    for (int iterator = start; iterator < (start + count); iterator++) {
      map.put(new MyInt(iterator), new MyInt(iterator));
    }

  }

  private void doSomeLiteralLiteralPuts(int start, int count) {
    for (int iterator = start; iterator < start + count; iterator++) {
      map.put(iterator, iterator);
    }
  }

  private void doSomeLiteralNonLiteralPuts(int start, int count) {
    for (int iterator = start; iterator < (start + count); iterator++) {
      map.put(iterator, new MyInt(iterator));
    }

  }

  }