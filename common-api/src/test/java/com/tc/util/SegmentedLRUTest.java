package com.tc.util;

import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

public class SegmentedLRUTest extends TestCase {

  public void testSanity() {

    SegmentedLRU lru = new SegmentedLRU<Object, String>(128);

    for (int i = 0; i < 200; i++) {
      lru.put(String.valueOf(i).hashCode(), String.valueOf(i) + "VALUE");
    }

    assertTrue(128 > lru.size());

    for (int i = 175; i < 200; i++) {
      assertTrue(lru.containsKey(String.valueOf(i).hashCode()));
    }

    int beforeSize = lru.size();

    assertTrue(lru.containsValue(String.valueOf(175) + "VALUE"));

    lru.remove(String.valueOf(175).hashCode());

    assertEquals(beforeSize - 1, lru.size());

    lru.clear();

    assertTrue(lru.isEmpty());

  }

  public void testMultiThreaded() throws InterruptedException {
    final int numOfLoops = 10000;
    int capacity = 16384;
    final int numOfProducers = 2;
    final int numOfConsumers = 2;
    final SegmentedLRU<Object, String> lru = new SegmentedLRU<Object, String>(capacity);

    Thread[] producerThreads = new Thread[numOfProducers];

    for (int i = 0; i < numOfProducers; i++) {
      producerThreads[i] = new Thread(new Producer(i, lru, numOfLoops), "producer " + i);
      producerThreads[i].start();
    }

    Thread[] consumerThreads = new Thread[numOfConsumers];

    for (int i = 0; i < numOfConsumers; i++) {
      consumerThreads[i] = new Thread(new Consumer(i, lru, numOfLoops), "consumer " + i);
      consumerThreads[i].start();
    }

    for (int i = 0; i < numOfProducers; i++) {
      producerThreads[i].join();
    }

    for (int i = 0; i < numOfConsumers; i++) {
      consumerThreads[i].join();
    }

    Assert.eval(lru.size() <= capacity);
  }

  private static class Producer implements Runnable {

    private final SegmentedLRU<Object, String> lru;
    private final int                          numOfLoops;
    private final int                          id;

    public Producer(int id, SegmentedLRU lru, int numOfLoops) {
      this.id = id;
      this.lru = lru;
      this.numOfLoops = numOfLoops;
    }

    public void run() {
      int start = id * numOfLoops;
      for (int i = start; i < start + numOfLoops; i++) {
        lru.put(new Integer(i), Integer.toString(i));
      }
    }

  }

  private static class Consumer implements Runnable {

    private final SegmentedLRU<Object, String> lru;
    private final int                          numOfLoops;
    private final int                          id;

    public Consumer(int id, SegmentedLRU lru, int numOfLoops) {
      this.id = id;
      this.lru = lru;
      this.numOfLoops = numOfLoops;
    }

    public void run() {
      int start = id * numOfLoops;
      for (int i = start; i < start + numOfLoops; i++) {
        String val = lru.get(new Integer(i));
        if (val != null) {
          Assert.eval(val.equals(Integer.toString(i)));
          ThreadUtil.reallySleep(1);
        }
      }

    }

  }
}