/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class BoundedBytesConcurrentHashMapTest extends TCTestCase {

  private class MyBoundedBytesConcurrentHashMap<K, V> extends BoundedBytesConcurrentHashMap<K, V> {

    public MyBoundedBytesConcurrentHashMap(long limit) {
      super(limit);
    }

    @Override
    public long getKeySize(K key) {
      if (key instanceof String) {
        return ((String) key).length();
      } else {
        throw new AssertionError("Add the new type");
      }
    }

    @Override
    public long getValueSize(V value) {
      if (value instanceof Integer) {
        return Integer.SIZE / Byte.SIZE;
      } else if (value instanceof String) {
        return ((String) value).length();
      } else {
        throw new AssertionError("Add the new type");
      }
    }

  }

  public void testBasic() {

    BoundedBytesConcurrentHashMap<String, Integer> boundedBytesConcurrentHashMap = new MyBoundedBytesConcurrentHashMap<String, Integer>(
                                                                                                                                        1600L);
    HashMap copyMap = new HashMap<String, Integer>();

    // put
    for (int i = 100; i < 200; i++) {
      Assert.assertNull(boundedBytesConcurrentHashMap.put("key" + i, i));
    }

    copyMap.putAll(boundedBytesConcurrentHashMap);

    int addCount = 0;
    // get
    for (int i = 100; i < 200; i++) {
      Assert.assertNotNull(boundedBytesConcurrentHashMap.get("key" + i));
      Assert.assertTrue(boundedBytesConcurrentHashMap.get("key" + i).equals(Integer.valueOf(i)));
      addCount++;
    }

    int removeCount = 0;
    // remove
    for (int i = 100; i < 200; i += 10) {
      Assert.assertTrue(boundedBytesConcurrentHashMap.remove("key" + i) != null);
      removeCount++;
    }

    // get after remove
    for (int i = 100; i < 200; i += 10) {
      Assert.assertTrue(boundedBytesConcurrentHashMap.get("key" + i) == null);
    }

    // iterator traverse
    for (Entry<String, Integer> entry : boundedBytesConcurrentHashMap.entrySet()) {
      System.out.println(entry.getKey() + " ==> " + entry.getValue());
    }

    Assert.assertEquals((addCount - removeCount) * (6 + 4), boundedBytesConcurrentHashMap.getSizeInBytes());

    // iterator remove
    Iterator iterator = boundedBytesConcurrentHashMap.entrySet().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }

    Assert.assertEquals(0, boundedBytesConcurrentHashMap.size());
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.putAll(copyMap);
    Assert.assertEquals(copyMap.size(), boundedBytesConcurrentHashMap.size());
    Assert.assertEquals(copyMap.size() * (6 + 4), boundedBytesConcurrentHashMap.getSizeInBytes());

  }

  public void testBoundedPutAndRemove() {
    final BoundedBytesConcurrentHashMap<String, Integer> boundedBytesConcurrentHashMap = new MyBoundedBytesConcurrentHashMap<String, Integer>(
                                                                                                                                              1000L);

    System.err.println("testBounded Map total size is " + boundedBytesConcurrentHashMap.getMaxSize());

    Runnable runnable = new Runnable() {
      public void run() {
        for (int i = 100; i < 300; i++) {
          System.err.println("testBounded Put " + ("key" + i) + "; currentSize : "
                             + boundedBytesConcurrentHashMap.getSizeInBytes());
          Assert.assertNull(boundedBytesConcurrentHashMap.put("key" + i, i));
        }
      }
    };

    Thread t = new Thread(runnable);
    t.start();

    ThreadUtil.reallySleep(1000);
    Assert.assertTrue(t.isAlive());

    for (int i = 100; i < 300; i++) {
      Object o;
      do {
        o = null;
        o = boundedBytesConcurrentHashMap.remove("key" + i);
        System.err.println("Remove " + ("key" + i) + " : " + o);
      } while (o == null);
    }

    ThreadUtil.reallySleep(1000);
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.size());
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    Assert.assertFalse(t.isAlive());
  }

  public void testBoundedIterator() {
    final BoundedBytesConcurrentHashMap<String, Integer> boundedBytesConcurrentHashMap = new MyBoundedBytesConcurrentHashMap<String, Integer>(
                                                                                                                                              517L);

    System.err.println("Segment size is " + boundedBytesConcurrentHashMap.getMaxSize());

    Runnable runnable = new Runnable() {
      public void run() {
        for (int i = 100; i < 300; i++) {
          Assert.assertNull(boundedBytesConcurrentHashMap.put("key" + i, i));
          System.err.println("testBoundedIterator Put success - " + ("key" + i) + "; currentSize: "
                             + boundedBytesConcurrentHashMap.getSizeInBytes());
        }
      }
    };

    Thread t = new Thread(runnable);
    t.start();

    ThreadUtil.reallySleep(1000);
    Assert.assertTrue(t.isAlive());

    while (boundedBytesConcurrentHashMap.size() != 0 || t.isAlive()) {
      Iterator<Entry<String, Integer>> iterator = boundedBytesConcurrentHashMap.entrySet().iterator();
      while (iterator.hasNext()) {
        // Assert.eval(boundedBytesConcurrentHashMap.getSizeInBytes() <= maxSize);
        Entry<String, Integer> entry = iterator.next();
        iterator.remove();
        System.err.println("testBoundedIterator Remove success : " + entry.getKey() + "; currentSize : "
                           + boundedBytesConcurrentHashMap.getSizeInBytes());
      }
    }

    ThreadUtil.reallySleep(1000);
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.size());
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    Assert.assertFalse(t.isAlive());
  }

  public void testBoundedClear() {
    final BoundedBytesConcurrentHashMap<String, Integer> boundedBytesConcurrentHashMap = new MyBoundedBytesConcurrentHashMap<String, Integer>(
                                                                                                                                              32L);

    Runnable runnable = new Runnable() {
      public void run() {
        for (int i = 0; i < 64; i++) {
          Assert.assertNull(boundedBytesConcurrentHashMap.put("key" + i, i));
          System.err.println("testBoundedClear Put success " + i);
        }
      }
    };

    Thread t = new Thread(runnable);
    t.start();

    ThreadUtil.reallySleep(1000);
    Assert.assertTrue(t.isAlive());

    while (t.isAlive()) {
      boundedBytesConcurrentHashMap.clear();
    }

    boundedBytesConcurrentHashMap.clear();
    Assert.assertFalse(t.isAlive());

    Assert.assertEquals(0, boundedBytesConcurrentHashMap.size());
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

  }

  public void testAccounting() {
    final BoundedBytesConcurrentHashMap<String, String> boundedBytesConcurrentHashMap = new MyBoundedBytesConcurrentHashMap<String, String>(
                                                                                                                                            1000L);
    // put
    boundedBytesConcurrentHashMap.put("key-1", "value-1");
    Assert.assertEquals(12, boundedBytesConcurrentHashMap.getSizeInBytes());

    // overwrite
    boundedBytesConcurrentHashMap.put("key-1", "value-2");
    Assert.assertEquals(12, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-1", "value-22");
    Assert.assertEquals(13, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-1", "value-");
    Assert.assertEquals(11, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-1", "");
    Assert.assertEquals(5, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-1", "value-3");
    Assert.assertEquals(12, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-2", "value-4");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // put if absent -- false case
    boundedBytesConcurrentHashMap.putIfAbsent("key-2", "value-5");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // remove
    boundedBytesConcurrentHashMap.remove("key-2");
    Assert.assertEquals(12, boundedBytesConcurrentHashMap.getSizeInBytes());

    boundedBytesConcurrentHashMap.put("key-2", "value-4");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // remove K,V -- false case
    boundedBytesConcurrentHashMap.remove("key-2", "value-5");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // remove K,V -- true case
    boundedBytesConcurrentHashMap.remove("key-2", "value-4");
    Assert.assertEquals(12, boundedBytesConcurrentHashMap.getSizeInBytes());

    // put if absent -- true case
    boundedBytesConcurrentHashMap.putIfAbsent("key-3", "value-6");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace -- true case
    boundedBytesConcurrentHashMap.replace("key-3", "value-7");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace -- false case
    boundedBytesConcurrentHashMap.replace("key-4", "value-8");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace K,V -- true case
    boundedBytesConcurrentHashMap.replace("key-3", "value-7", "value-8");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace K,V -- false case
    boundedBytesConcurrentHashMap.replace("key-3", "value-7", "value-8");
    Assert.assertEquals(24, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace K,V -- true case -- larger value
    boundedBytesConcurrentHashMap.replace("key-3", "value-8", "value-88");
    Assert.assertEquals(25, boundedBytesConcurrentHashMap.getSizeInBytes());

    // replace K,V -- true case -- lesser value
    boundedBytesConcurrentHashMap.replace("key-3", "value-88", "v");
    Assert.assertEquals(18, boundedBytesConcurrentHashMap.getSizeInBytes());

    HashMap<String, String> backUp = new HashMap<String, String>(boundedBytesConcurrentHashMap);
    long origOldMapSize = boundedBytesConcurrentHashMap.getSizeInBytes();

    // entryset iterator remove
    Iterator<Entry<String, String>> i = boundedBytesConcurrentHashMap.entrySet().iterator();
    while (i.hasNext()) {
      Entry<String, String> e = i.next();
      long oldMapSize = boundedBytesConcurrentHashMap.getSizeInBytes();
      long entrySize = boundedBytesConcurrentHashMap.getKeySize(e.getKey())
                       + boundedBytesConcurrentHashMap.getValueSize(e.getValue());
      i.remove();
      long newMapSize = boundedBytesConcurrentHashMap.getSizeInBytes();
      Assert.assertEquals(oldMapSize - entrySize, newMapSize);
    }

    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    // put all
    backUp.size();
    boundedBytesConcurrentHashMap.putAll(backUp);
    Assert.assertEquals(origOldMapSize, boundedBytesConcurrentHashMap.getSizeInBytes());

    // keyset iterator remove
    Iterator<String> j = boundedBytesConcurrentHashMap.keySet().iterator();
    while (j.hasNext()) {
      String key = j.next();
      long oldMapSize = boundedBytesConcurrentHashMap.getSizeInBytes();
      String value = boundedBytesConcurrentHashMap.get(key);
      long entrySize = boundedBytesConcurrentHashMap.getKeySize(key)
                       + boundedBytesConcurrentHashMap.getValueSize(value);
      j.remove();
      long newMapSize = boundedBytesConcurrentHashMap.getSizeInBytes();
      Assert.assertEquals(oldMapSize - entrySize, newMapSize);
    }
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    // put all
    boundedBytesConcurrentHashMap.putAll(backUp);
    Assert.assertEquals(origOldMapSize, boundedBytesConcurrentHashMap.getSizeInBytes());

    // valueset iterator remove
    Iterator<String> k = boundedBytesConcurrentHashMap.values().iterator();
    while (k.hasNext()) {
      k.next();
      k.remove();
    }
    Assert.assertEquals(0, boundedBytesConcurrentHashMap.getSizeInBytes());

    // put all
    boundedBytesConcurrentHashMap.putAll(backUp);
    Assert.assertEquals(origOldMapSize, boundedBytesConcurrentHashMap.getSizeInBytes());

  }

}
